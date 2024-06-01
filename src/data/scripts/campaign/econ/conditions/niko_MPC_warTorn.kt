package data.scripts.campaign.econ.conditions

class niko_MPC_warTorn: niko_MPC_baseNikoCondition() {
    companion object {
        const val hazardIncrease = 0.25f
        const val defenseRatingMult = 0.7f

        const val techMiningArmamentsBase = 1f
        const val techMiningShipsBase = 2f
        const val techMiningMetalsBase = 3f
    }
}