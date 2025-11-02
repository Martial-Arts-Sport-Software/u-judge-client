package org.u_judge_client.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.attafitamim.krop.core.crop.rememberImageCropper
import io.github.suwasto.capturablecompose.CaptureController
import io.github.suwasto.capturablecompose.Capturable
import kotlinx.coroutines.launch

//@Composable
//fun CropImage(inputImage: ImageBitmap) {
//    val imageCropper = rememberImageCropper()
//
//    scope.launch {
//        val result = imageCropper.crop(bitmap) // Suspends until user accepts or cancels cropping
//        when (result) {
//            CropResult.Cancelled -> { }
//            is CropError -> { }
//            is CropResult.Success -> { result.bitmap }
//        }
//    }
//}


@Composable
fun ScreenshotWithCrop(content: @Composable () -> Unit) {
    val captureController = remember { CaptureController() }
    var screenshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var cropped by remember { mutableStateOf<ImageBitmap?>(null) }

    Capturable(
        captureController = captureController,
        onCaptured = { imageBitmap ->
            val cropRect = Rect(0f, 0f, imageBitmap.width / 2f, imageBitmap.height / 2f)
        },
        content = content
    )
}