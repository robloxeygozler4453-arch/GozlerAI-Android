package com.turkeyproje.gozlerai

data class ModelInfo(
    val id: String,
    val title: String,
    val version: String,
    val repository: String,
    val filename: String,
    val sizeLabel: String,
    val description: String
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$repository/resolve/main/$filename?download=true"

    companion object {
        val official = listOf(
            ModelInfo(
                "v2", "GözlerAI v2", "2.0",
                "TurkeyProje/GozlerAI-1B-v2-GGUF",
                "GozlerAI-v2-Q4_K_M.gguf", "~808 MB",
                "Güncel Türkçe LoRA sürümü · GGUF Q4_K_M"
            ),
            ModelInfo(
                "v1", "GözlerAI v1", "1.0",
                "TurkeyProje/GozlerAI-1B-Merged-GGUF",
                "GozlerAI-1B-Q4_K_M.gguf", "~808 MB",
                "İlk yayımlanan GGUF sürümü · Q4_K_M"
            )
        )
    }
}
