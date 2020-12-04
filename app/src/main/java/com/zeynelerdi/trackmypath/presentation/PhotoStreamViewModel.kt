package com.zeynelerdi.trackmypath.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch

import com.zeynelerdi.trackmypath.common.Result
import com.zeynelerdi.trackmypath.domain.usecase.RetrievePhotosUseCase
import com.zeynelerdi.trackmypath.presentation.model.PhotoViewItem
import com.zeynelerdi.trackmypath.presentation.model.toPresentationModel

import timber.log.Timber

class PhotoStreamViewModel(
    private val retrievePhotosUseCase: RetrievePhotosUseCase
) : ViewModel() {

    private val _photosByLocation = MutableLiveData<List<PhotoViewItem>>()
    val photosByLocation: LiveData<List<PhotoViewItem>>
        get() = _photosByLocation

    fun retrievePhotos() {
        viewModelScope.launch {
            when (val result = retrievePhotosUseCase.invoke()) {
                is Result.Success -> {
                    _photosByLocation.postValue(result.data.map { it.toPresentationModel() })
                }
                is Result.Error -> Timber.d("no photos")
            }
        }
    }
}
