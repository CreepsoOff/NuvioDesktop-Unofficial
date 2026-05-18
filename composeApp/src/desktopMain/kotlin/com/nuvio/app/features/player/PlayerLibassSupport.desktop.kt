package com.nuvio.app.features.player

// Desktop MPV supports ASS/SSA rendering through libass. This flag only hides the
// Android-specific libass toggle whose renderer choice does not map to MPV.
internal actual val platformShowsAndroidLibassToggle: Boolean = false
