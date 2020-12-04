package com.zeynelerdi.trackmypath.data.network

import com.zeynelerdi.trackmypath.common.Result

interface FlickrClient {

    suspend fun searchPhoto(
        lat: String,
        lon: String,
        radius: String
    ): Result<List<PhotoResponseEntity>>
}
