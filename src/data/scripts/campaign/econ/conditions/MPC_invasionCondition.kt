package data.scripts.campaign.econ.conditions

class MPC_invasionCondition: niko_MPC_baseNikoCondition() {

    var invasionInterval

    override fun apply(id: String) {
        super.apply(id)


    }

    override fun isTransient(): Boolean {
        return false
    }

}