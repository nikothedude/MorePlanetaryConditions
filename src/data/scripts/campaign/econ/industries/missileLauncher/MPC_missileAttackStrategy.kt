package data.scripts.campaign.econ.industries.missileLauncher

enum class MPC_missileAttackStrategy(
    //val weaponId: String
) {
    NUKE,
    KINETIC,
    FRAG_MIRV,
    EMP_MIRV,
    FLAK_MIRV;
}

/*
    Maybe two different attack types?
    Normal, special?

    Normal
        Consistant
        Single attacks

        Just a single nuke. A single spear. A single mirv. A single target
    Special
        Very strong
        A series of nukes, a line of spears

        Inconsistant, doesnt happen much

        Should maybe put normal on a small cooldown too?

    Or maybe normal attacks just vary in quantity...?
 */