package com.zeynelerdi.trackmypath.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.zeynelerdi.trackmypath.common.Result
import com.zeynelerdi.trackmypath.domain.PhotoRepository
import com.zeynelerdi.trackmypath.domain.model.Photo

class RetrievePhotosUseCase(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke(): Result<List<Photo>> = withContext(Dispatchers.IO) {
        return@withContext photoRepository.loadAllPhotos()
    }
}
