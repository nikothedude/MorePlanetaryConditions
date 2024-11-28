package data.scripts.campaign.magnetar.crisis.contribution

data class MPC_factionContributionChangeData(
    val contribution: MPC_factionContribution,
    val reason: MPC_changeReason,
    val lost: Boolean
) {
}