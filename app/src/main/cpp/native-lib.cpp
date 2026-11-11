#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include <thread>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "GozlerAI", __VA_ARGS__)

static llama_model * g_model = nullptr;
static llama_context * g_ctx = nullptr;
static std::mutex g_mutex;

static void release_model() {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_turkeyproje_gozlerai_NativeBridge_nativeLoadModel(JNIEnv * env, jobject, jstring path) {
    std::lock_guard<std::mutex> guard(g_mutex);
    const char * raw = env->GetStringUTFChars(path, nullptr);
    if (!raw) return JNI_FALSE;
    std::string model_path(raw);
    env->ReleaseStringUTFChars(path, raw);

    release_model();
    llama_backend_init();

    auto model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (!g_model) {
        LOGE("Failed to load GGUF: %s", model_path.c_str());
        return JNI_FALSE;
    }

    auto ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 512;
    ctx_params.n_threads = std::max(2, (int) std::thread::hardware_concurrency() - 1);
    ctx_params.n_threads_batch = ctx_params.n_threads;
    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create llama context");
        release_model();
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_turkeyproje_gozlerai_NativeBridge_nativeGenerate(JNIEnv * env, jobject, jstring prompt, jint max_tokens) {
    std::lock_guard<std::mutex> guard(g_mutex);
    if (!g_model || !g_ctx) return env->NewStringUTF("HATA: Önce bir GGUF modeli yüklemelisin.");

    const char * raw = env->GetStringUTFChars(prompt, nullptr);
    if (!raw) return env->NewStringUTF("");
    std::string input(raw);
    env->ReleaseStringUTFChars(prompt, raw);

    const llama_vocab * vocab = llama_model_get_vocab(g_model);
    int32_t needed = -llama_tokenize(vocab, input.c_str(), (int32_t) input.size(), nullptr, 0, true, true);
    if (needed <= 0) return env->NewStringUTF("HATA: İstek token'lara dönüştürülemedi.");
    std::vector<llama_token> tokens((size_t) needed);
    int32_t count = llama_tokenize(vocab, input.c_str(), (int32_t) input.size(), tokens.data(), needed, true, true);
    if (count < 0) return env->NewStringUTF("HATA: Tokenizer hatası.");
    tokens.resize((size_t) count);
    if (tokens.empty()) return env->NewStringUTF("");

    llama_memory_clear(llama_get_memory(g_ctx), true);
    const int n_batch = 512;
    int pos = 0;
    while (pos < (int) tokens.size()) {
        int n = std::min(n_batch, (int) tokens.size() - pos);
        llama_batch batch = llama_batch_init(n, 0, 1);
        for (int i = 0; i < n; ++i) {
            batch.token[i] = tokens[(size_t) pos + i];
            batch.pos[i] = pos + i;
            batch.n_seq_id[i] = 1;
            batch.seq_id[i][0] = 0;
            batch.logits[i] = (i == n - 1) ? 1 : 0;
        }
        int result = llama_decode(g_ctx, batch);
        llama_batch_free(batch);
        if (result != 0) return env->NewStringUTF("HATA: Model girdiyi işleyemedi.");
        pos += n;
    }

    auto sampler_params = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.90f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.75f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string output;
    const int limit = std::max(1, std::min((int) max_tokens, 512));
    for (int i = 0; i < limit; ++i) {
        llama_token token = llama_sampler_sample(sampler, g_ctx, -1);
        llama_sampler_accept(sampler, token);
        if (llama_vocab_is_eog(vocab, token)) break;

        char piece[256];
        int piece_len = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
        if (piece_len > 0) output.append(piece, (size_t) piece_len);

        llama_batch batch = llama_batch_init(1, 0, 1);
        batch.token[0] = token;
        batch.pos[0] = pos++;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        int result = llama_decode(g_ctx, batch);
        llama_batch_free(batch);
        if (result != 0) break;
    }
    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_turkeyproje_gozlerai_NativeBridge_nativeUnload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> guard(g_mutex);
    release_model();
}
