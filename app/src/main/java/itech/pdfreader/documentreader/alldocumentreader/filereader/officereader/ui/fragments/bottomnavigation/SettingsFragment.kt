package itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.ui.fragments.bottomnavigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.R
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.databinding.FragmentSettingsBinding
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.ui.activities.DashboardActivity
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.ui.dialogs.RateDialogNew
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.ui.fragments.BaseFragment
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.uitilities.*

class SettingsFragment : BaseFragment() {

    lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentSettingsBinding.inflate(layoutInflater)

        binding.privacyBtn.setOnClickListener {
            requireActivity().openUrl("https://itechsolutionapps.wordpress.com/")
        }
        binding.rateUsBtn.setOnClickListener {
            RateDialogNew(requireActivity()).createRateUsDialog(false,sharedPref)
        }
        binding.shareBtn.setOnClickListener {
            requireActivity().shareApp()
        }
        binding.restorePurchaseBtn.setOnClickListener {
            binding.root.snack("Coming soon")
            //requireActivity().openActivity(SubscriptionActivity::class.java)
        }

        refreshAdOnView()

        return binding.root
    }

    private val TAG = "SettingsFragment"

    private fun refreshAdOnView() {
        /**show native ad*/
        /************************/
        binding.adLayout.visibility = View.GONE
        binding.shimmerViewContainer.visibility = View.GONE
        binding.shimmerViewContainer.stopShimmer()
    }
}