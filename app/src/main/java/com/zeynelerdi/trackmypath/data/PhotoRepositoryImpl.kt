package com.zeynelerdi.trackmypath.data

import java.io.IOException
import java.lang.Exception

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.zeynelerdi.trackmypath.common.Result
import com.zeynelerdi.trackmypath.data.database.PhotoDao
import com.zeynelerdi.trackmypath.data.database.PhotoEntity
import com.zeynelerdi.trackmypath.data.database.toDomainModel
import com.zeynelerdi.trackmypath.data.network.toDomainModel
import com.zeynelerdi.trackmypath.data.network.toPhotoEntity
import com.zeynelerdi.trackmypath.data.network.FlickrClient
import com.zeynelerdi.trackmypath.data.network.PhotoResponseEntity
import com.zeynelerdi.trackmypath.domain.model.Photo
import com.zeynelerdi.trackmypath.domain.PhotoRepository

import timber.log.Timber

class PhotoRepositoryImpl(
    private val flickrClient: FlickrClient,
    private val photoDao: PhotoDao
) : PhotoRepository {

    override suspend fun searchPhotoByLocation(lat: String, lon: String): Result<Photo> {
        return try {
            // Radius used for geo queries, greater than zero and less than 20 miles (or 32 kilometers),
            // for use with point-based geo queries. The default value is 5 (km).
            // Set a radius of 100 meters. (default unit is km)
            return when (val response = flickrClient.searchPhoto(lat, lon, "0.1")) {
                is Result.Success -> {
                    val photosFromDb = photoDao.selectAllPhotos()
                    // if photo exists in DB, then take the next from the response
                    val photoFromFlickr = findUniquePhoto(response.data, photosFromDb)
                    photoFromFlickr?.let {
                        // save it in the DB
                        photoDao.insert(photoFromFlickr.toPhotoEntity())
                        Result.Success(photoFromFlickr.toDomainModel())
                    } ?: throw IOException("no photos retrieved from flickr")
                }
                is Result.Error -> {
                    Result.Error(response.exception)
                }
                else -> Result.Error(Exception("unknown exception"))
            }
        } catch (e: Exception) {
            Timber.e(e, "searchPhotoByLocation exception")
            Result.Error(IOException("searchPhotoByLocation exception", e))
        }
    }

    // Retrieve all photos from the DB
    override suspend fun loadAllPhotos(): Result<List<Photo>> {
        val photos = photoDao.selectAllPhotos()
        return if (photos.isNotEmpty()) {
            val result = photos.map { photoEntity -> photoEntity.toDomainModel() }
            Result.Success(result)
        } else {
            Result.Error(IOException("Failed to retrieve photos from database"))
        }
    }

    // Delete all photos in DB
    override suspend fun deletePhotos() {
        try {
            photoDao.deletePhotos()
        } catch (e: Exception) {
            Timber.e(e, "deletePhotos exception")
        }
    }

    private suspend fun findUniquePhoto(
        photosFromFlickr: List<PhotoResponseEntity>,
        photosFromDb: Array<PhotoEntity>
    ): PhotoResponseEntity? {
        return withContext(Dispatchers.Default) {
            if (photosFromDb.isNotEmpty()) {
                val iterator = photosFromFlickr.iterator()
                while (iterator.hasNext()) {
                    val photoResponseEntity = iterator.next()
                    Timber.d("fetched photo id ${photoResponseEntity.id}")
                    if (photosFromDb.all { photoEntity -> photoEntity.id != photoResponseEntity.id }) {
                        return@withContext photoResponseEntity
                    }
                }
            } else {
                return@withContext photosFromFlickr[0]
            }
            return@withContext null
        }
    }
}
