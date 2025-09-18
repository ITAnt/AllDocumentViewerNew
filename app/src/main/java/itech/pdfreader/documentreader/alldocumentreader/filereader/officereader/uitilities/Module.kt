package itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.uitilities

import androidx.annotation.Keep
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.remote.LocalConfigDataClass
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.viewmodels.DataViewModel
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.viewmodels.UtilsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


@Keep
object AppModule {
    val getModule = module {
        //single { DocModelRepository(get()) }
        single { SharedPref(get()) }
        single { DocumentUtils(get()) }
        single { LocalConfigDataClass().init() }

//        viewModel { BillingViewModel(application = get()) }
        viewModel { DataViewModel(get()) }
        viewModel { UtilsViewModel(get()) }
    }
}
