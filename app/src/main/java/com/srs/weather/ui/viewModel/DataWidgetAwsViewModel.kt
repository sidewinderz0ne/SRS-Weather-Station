package com.srs.weather.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.srs.weather.data.model.DataWidgetAwsModel
import com.srs.weather.data.repository.DataWidgetAwsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataWidgetAwsViewModel(application: Application, private val widgetAwsRepo: DataWidgetAwsRepository) : AndroidViewModel(application) {

    private val _insertionResult = MutableLiveData<Boolean>()
    val insertionResult: LiveData<Boolean> get() = _insertionResult

    private val _dataWidgetAwsList = MutableLiveData<List<DataWidgetAwsModel>>()
    val dataWidgetAwsList: LiveData<List<DataWidgetAwsModel>> get() = _dataWidgetAwsList

    fun insertDataWidgetAws(rdata: String, widgetId: Int) {
        viewModelScope.launch {
            try {
                val dataWidgetAws = DataWidgetAwsModel(rdata, widgetId)
                val isInserted = widgetAwsRepo.insertDataWidgetAws(dataWidgetAws)
                _insertionResult.value = isInserted
            } catch (e: Exception) {
                e.printStackTrace()
                _insertionResult.value = false
            }
        }
    }

    fun deleteDataWidgetAws(idWidget: String? = "") {
        viewModelScope.launch {
            widgetAwsRepo.deleteDataWidgetAws(idWidget)
        }
    }

    fun getCountDataWidgetAws(): Int {
        return widgetAwsRepo.getCountDataWidgetAws()
    }

    fun loadDataWidgetAws() {
        viewModelScope.launch {
            val dataWidgetAws = withContext(Dispatchers.IO) {
                widgetAwsRepo.getAllDataWidgetAws()
            }
            _dataWidgetAwsList.value = dataWidgetAws
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application, private val widgetAwsRepo: DataWidgetAwsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DataWidgetAwsViewModel::class.java)) {
                return DataWidgetAwsViewModel(application, widgetAwsRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}