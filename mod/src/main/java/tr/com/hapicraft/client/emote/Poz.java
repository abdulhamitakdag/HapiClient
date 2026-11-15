package tr.com.hapicraft.client.emote;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Bir karedeki gövde duruşu. Açılar radyan; kök ötelemesi model pikseli (aşağı +). */
public final class Poz {
    public float basX, basY, basZ;
    public float govdeX, govdeY, govdeZ;
    public float sagKolX, sagKolY, sagKolZ;
    public float solKolX, solKolY, solKolZ;
    public float sagBacakX, sagBacakY, sagBacakZ;
    public float solBacakX, solBacakY, solBacakZ;
    public float kokY, kokDonus, kokEgim, kokYatis;

    public void oku(HumanoidModel<?> m) {
        basX = m.head.xRot; basY = m.head.yRot; basZ = m.head.zRot;
        govdeX = m.body.xRot; govdeY = m.body.yRot; govdeZ = m.body.zRot;
        sagKolX = m.rightArm.xRot; sagKolY = m.rightArm.yRot; sagKolZ = m.rightArm.zRot;
        solKolX = m.leftArm.xRot; solKolY = m.leftArm.yRot; solKolZ = m.leftArm.zRot;
        sagBacakX = m.rightLeg.xRot; sagBacakY = m.rightLeg.yRot; sagBacakZ = m.rightLeg.zRot;
        solBacakX = m.leftLeg.xRot; solBacakY = m.leftLeg.yRot; solBacakZ = m.leftLeg.zRot;
        kokY = 0; kokDonus = 0; kokEgim = 0; kokYatis = 0;
    }

    /** Vanilla duruşu ile dans duruşu arasında agirlik (0..1) kadar geçiş yapıp modele yazar. */
    public void yaz(HumanoidModel<?> m, Poz v, float w) {
        parca(m.head, v.basX, v.basY, v.basZ, basX, basY, basZ, w);
        parca(m.body, v.govdeX, v.govdeY, v.govdeZ, govdeX, govdeY, govdeZ, w);
        parca(m.rightArm, v.sagKolX, v.sagKolY, v.sagKolZ, sagKolX, sagKolY, sagKolZ, w);
        parca(m.leftArm, v.solKolX, v.solKolY, v.solKolZ, solKolX, solKolY, solKolZ, w);
        parca(m.rightLeg, v.sagBacakX, v.sagBacakY, v.sagBacakZ, sagBacakX, sagBacakY, sagBacakZ, w);
        parca(m.leftLeg, v.solBacakX, v.solBacakY, v.solBacakZ, solBacakX, solBacakY, solBacakZ, w);
        ModelPart kok = m.root();
        kok.y += kokY * w;
        kok.yRot += kokDonus * w;
        kok.xRot += kokEgim * w;
        kok.zRot += kokYatis * w;
    }

    private static void parca(ModelPart p, float vx, float vy, float vz, float x, float y, float z, float w) {
        p.xRot = vx + (x - vx) * w;
        p.yRot = vy + (y - vy) * w;
        p.zRot = vz + (z - vz) * w;
    }

    public void kollar(float x, float y, float z) {
        sagKolX = x; sagKolY = y; sagKolZ = z;
        solKolX = x; solKolY = -y; solKolZ = -z;
    }

    public void bacaklar(float x, float y, float z) {
        sagBacakX = x; sagBacakY = y; sagBacakZ = z;
        solBacakX = x; solBacakY = -y; solBacakZ = -z;
    }

    public void dur() {
        kollar(0, 0, 0);
        bacaklar(0, 0, 0);
        govdeX = govdeY = govdeZ = 0;
    }
}
