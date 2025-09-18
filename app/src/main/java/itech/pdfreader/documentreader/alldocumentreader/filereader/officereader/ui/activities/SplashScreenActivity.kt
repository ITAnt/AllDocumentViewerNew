package itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.ui.activities

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.databinding.ActivitySplashBinding
import androidx.annotation.Keep
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.uitilities.*
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.uitilities.Companions.Companion.isPurchased

@Keep
class SplashScreenActivity : BaseActivity() {
    lateinit var binding: ActivitySplashBinding
    private val TAG = "SplashActivity"

//    private val billingViewModel: BillingViewModel by viewModel()

    private var isSplashTimerComplete = false
    private var isFileLoadingComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)

        initApplication()

        setOnClickListeners()
        setContentView(binding.root)
    }

    private fun initApplication() {
        hideStatusBar()

//        billingViewModel.subSkuDetailsListLiveData.observe(this, { skuList ->
//            if (!skuList.isNullOrEmpty()) {
//                if (!skuList[0].canPurchase || !skuList[1].canPurchase) {
//                    utilsViewModel.setPremiumUser(true)
//                    utilsViewModel.setAutoAdsRemoved(true)
//                    isPurchased = true
//                } else
//                    initNotPurchase()
//            }
//        })

        createMyFilesDirs()

        if (utilsViewModel.checkPermission(this)) {
            dataViewModel.retrieveFilesFormDevice {
                isFileLoadingComplete = true
                if (sharedPref.getBoolean(Constants.isGetStartedBtnClick)) {
                    navigateToNextScreen()
                } else {
                    binding.loadingTxt.visibility = View.GONE
                    binding.getStartBtn.visibility = View.VISIBLE
                }
            }
        } else
            isFileLoadingComplete = true

        initNotPurchase()
    }

    private fun initNotPurchase() {
        utilsViewModel.setPremiumUser(false)
        utilsViewModel.setAutoAdsRemoved(false)
        isPurchased = false
        utilsViewModel.syncRemoteConfig()
    }

    override fun onResume() {
        super.onResume()
        applySplashTimer()
    }




    private fun applySplashTimer() {
        startWithTwoSecondsDelay()
    }

    private fun startWithTwoSecondsDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            isSplashTimerComplete = true
            if (sharedPref.getBoolean(Constants.isGetStartedBtnClick)) {
                navigateToNextScreen()
            } else {
                binding.loadingTxt.visibility = View.GONE
                binding.getStartBtn.visibility = View.VISIBLE
            }
        }, 2000)
    }


    private fun setOnClickListeners() {
        binding.getStartBtn.setOnClickListener {
            sharedPref.putBoolean(Constants.isGetStartedBtnClick, true)
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        if (isFileLoadingComplete && isSplashTimerComplete) {
            checkPermissionAndNavigateToMainActivity()
        }
    }

    private fun checkPermissionAndNavigateToMainActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!utilsViewModel.checkPermission(this))
                openActivityWithClearTask(PermissionActivity::class.java)
            else
                openActivityWithClearTask(DashboardActivity::class.java)
        } else {
            openActivityWithClearTask(PermissionActivity::class.java)
        }
    }
}