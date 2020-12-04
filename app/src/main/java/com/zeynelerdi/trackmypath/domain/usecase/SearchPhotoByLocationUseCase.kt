package com.zeynelerdi.trackmypath.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.zeynelerdi.trackmypath.common.Result
import com.zeynelerdi.trackmypath.domain.PhotoRepository
import com.zeynelerdi.trackmypath.domain.model.Photo

class SearchPhotoByLocationUseCase(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke(lat: Double, lon: Double): Result<Photo> = withContext(Dispatchers.IO) {
        return@withContext photoRepository.searchPhotoByLocation(lat.toString(), lon.toString())
    }
}
