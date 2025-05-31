package data.scripts.campaign.econ

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.econ.MonthlyReport
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import data.utilities.niko_MPC_ids.INCOME_TALLY_ID

class MPC_incomeTallyListener: BaseCampaignEventListener(false) {

    class MPC_incomeTally {

        companion object {
            fun get(withUpdate: Boolean = false): MPC_incomeTally? {
                if (withUpdate && Global.getSector().memoryWithoutUpdate[INCOME_TALLY_ID] !is MPC_incomeTally) {
                    Global.getSector().memoryWithoutUpdate[INCOME_TALLY_ID] = MPC_incomeTally()
                }
                return Global.getSector().memoryWithoutUpdate[INCOME_TALLY_ID] as? MPC_incomeTally
            }
            const val MAX_STORE_SIZE = 7 // only store the last x reports
        }
        private val tally = ArrayList<Float>()

        fun addTally(report: MonthlyReport) {
            tally += calculateTotalIncome(report)
            if (tally.size > MAX_STORE_SIZE) {
                tally.removeAt(0)
            }
        }

        private fun calculateTotalIncome(report: MonthlyReport): Float {
            return (report.root.totalIncome - report.root.totalUpkeep)
        }

        fun getHighestIncome(): Float {
            var highest = 0f
            for (report in tally) {
                if (report > highest) {
                    highest = report
                }
            }
            return highest
        }
    }

    override fun reportEconomyMonthEnd() {
        super.reportEconomyMonthEnd()

        val report = SharedData.getData().previousReport // at this point, corescript should have run
        MPC_incomeTally.get(true)?.addTally(report)
    }
}