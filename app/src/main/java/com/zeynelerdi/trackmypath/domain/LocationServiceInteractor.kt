package com.zeynelerdi.trackmypath.domain

import com.zeynelerdi.trackmypath.common.Result
import com.zeynelerdi.trackmypath.domain.model.Photo
import com.zeynelerdi.trackmypath.domain.usecase.ClearPhotosUseCase
import com.zeynelerdi.trackmypath.domain.usecase.SearchPhotoByLocationUseCase

class LocationServiceInteractor(
    private val clearPhotosUseCase: ClearPhotosUseCase,
    private val searchPhotoByLocationUseCase: SearchPhotoByLocationUseCase
) {

    suspend fun clearPhotosFromList() {
        clearPhotosUseCase.invoke()
    }

    suspend fun getPhotoBasedOnLocation(latitude: Double, longitude: Double): Result<Photo> {
        return searchPhotoByLocationUseCase.invoke(latitude, longitude)
    }
}
