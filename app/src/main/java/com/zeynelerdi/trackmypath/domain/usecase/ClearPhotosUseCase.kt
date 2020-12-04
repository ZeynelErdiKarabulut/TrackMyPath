package com.zeynelerdi.trackmypath.domain.usecase

import com.zeynelerdi.trackmypath.domain.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearPhotosUseCase(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        photoRepository.deletePhotos()
    }
}
