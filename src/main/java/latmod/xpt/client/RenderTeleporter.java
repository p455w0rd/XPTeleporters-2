package latmod.xpt.client;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import latmod.xpt.block.TileTeleporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTeleporter extends TileEntitySpecialRenderer<TileTeleporter> {

	private static Minecraft mc = Minecraft.getMinecraft();

	@Override
	public void render(final TileTeleporter t, final double rx, final double ry, final double rz, final float pt, final int destroyStage, final float alphaChannel) {
		final int ID = t.getType();
		if (ID == 0) {
			return;
		}
		double dx = t.getPos().getX() + 0.5 - mc.getRenderManager().viewerPosX;
		dx *= dx;
		double dy = t.getPos().getY() + 0.125 - mc.getRenderManager().viewerPosY;
		dy *= dy;
		double dz = t.getPos().getZ() + 0.5 - mc.getRenderManager().viewerPosZ;
		dz *= dz;
		final double dist = Math.sqrt(dx + dy + dz);
		if (dx <= 0.0 && dz <= 0.0) {
			return;
		}
		float alpha = (float) getAlpha(dist) + 0.05F;
		if (alpha <= 0F) {
			return;
		}
		if (alpha > 1F) {
			alpha = 1F;
		}
		// nfi what the point of the following line is
		//if(te.getWorldObj().rand.nextInt(100) > 97) return;
		final double cooldown = t.cooldown > 0 ? 1.0 - (double) t.cooldown / (double) t.maxCooldown : 1.0;
		GL11.glPushMatrix();
		GL11.glPushAttrib(8192);
		final float lastBrightnessX = OpenGlHelper.lastBrightnessX;
		final float lastBrightnessY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 0.0f);
		GL11.glTranslated(rx + 0.5, ry + 0.0, rz + 0.5);
		GL11.glScalef(-1.0f, -1.0f, 1.0f);
		GL11.glRotated(-Math.atan2(t.getPos().getX() + 0.5 - mc.getRenderManager().viewerPosX, t.getPos().getZ() + 0.5 - mc.getRenderManager().viewerPosZ) * 180.0 / 3.141592653589793, 0.0, 1.0, 0.0);
		GL11.glDisable(2896);
		GL11.glDisable(2884);
		GL11.glDepthMask(false);
		GL11.glDisable(3553);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		GL11.glEnable(3042);
		GL11.glBlendFunc(770, 1);
		GL11.glDisable(3008);
		GL11.glShadeModel(7425);
		GL11.glColor4f(0.0f, 1.0f, 1.0f, 0.0f);
		GL11.glBegin(7);
		final double s = cooldown * 0.75;
		final double s1 = cooldown * 0.12;
		GL11.glVertex3d(-s, -1.5, 0.0);
		GL11.glVertex3d(s, -1.5, 0.0);
		if (ID == 1) {
			GL11.glColor4f(0.2f, 0.4f, 1.0f, alpha);
		}
		else {
			GL11.glColor4f(0.2f, 1.0f, 0.2f, alpha * 0.8f);
		}
		GL11.glVertex3d(s1, -0.125, 0.0);
		GL11.glVertex3d(-s1, -0.125, 0.0);
		GL11.glEnd();
		if (t.cooldown > 0) {
			final double b = 0.03125;
			GL11.glColor4f(0.3372549f, 0.85490197f, 1.0f, alpha * 0.3f);
			final double w = 2.0;
			final double h = 0.25;
			final double x = -w / 2.0;
			final double y = -2.15 - h / 2.0;
			drawRect(x, y, w, b / 2.0, 0.0);
			drawRect(x, y + h - b / 2.0, w, b / 2.0, 0.0);
			drawRect(x, y + b / 2.0, b / 2.0, h - b, 0.0);
			drawRect(x + w - b / 2.0, y + b / 2.0, b / 2.0, h - b, 0.0);
			GL11.glColor4f(0.0f, 1.0f, 0.0f, alpha * 0.25f);
			final double w1 = w * cooldown;
			drawRect(x + b, y + b, w1 - b * 2.0, h - b * 2.0, 0.0);
			GL11.glColor4f(1.0f, 0.0f, 0.0f, alpha * 0.25f);
			final double w2 = w1 - b * 2.0;
			drawRect(w2 + x + b, y + b, w - b * 2.0 - w2, h - b * 2.0, 0.0);
		}
		GL11.glDepthMask(true);
		GL11.glPopAttrib();
		GL11.glPopMatrix();
		if (alpha > 0.05f) {
			final String name = t.getLinkedDimPosDesc();
			GL11.glPushMatrix();
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
			GL11.glTranslated(rx + 0.5, ry + 1.6, rz + 0.5);
			GL11.glNormal3f(0.0f, 1.0f, 0.0f);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_CURRENT_BIT);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			//GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_LIGHTING);
			final float f1 = 0.02f;
			GL11.glScalef(-f1, -f1, f1);
			GL11.glRotated(-Math.atan2(t.getPos().getX() + 0.5 - mc.getRenderManager().viewerPosX, t.getPos().getZ() + 0.5 - mc.getRenderManager().viewerPosZ) * 180.0 / 3.141592653589793, 0.0, 1.0, 0.0);
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			final String[] lines = name.split("\n");
			mc.fontRenderer.drawString(lines[0], -mc.fontRenderer.getStringWidth(lines[0]) / 2, -18, new Color(255, 255, 255, (int) (alpha * 255.0f + 0.5f)).getRGB());
			mc.fontRenderer.drawString(lines[1], -mc.fontRenderer.getStringWidth(lines[1]) / 2, -8, new Color(255, 255, 255, (int) (alpha * 255.0f + 0.5f)).getRGB());
			//GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glPopAttrib();
			GL11.glPopMatrix();
		}
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
	}

	private void drawQuad(final double x1, final double y1, final double x2, final double y2, final double z) {
		GL11.glBegin(7);
		GL11.glVertex3d(x1, y1, z);
		GL11.glVertex3d(x2, y1, z);
		GL11.glVertex3d(x2, y2, z);
		GL11.glVertex3d(x1, y2, z);
		GL11.glEnd();
	}

	private void drawRect(final double x, final double y, final double w, final double h, final double z) {
		drawQuad(x, y, x + w, y + h, z);
	}

	private double getAlpha(final double dist) {
		if (dist < 2.0) {
			return dist / 2.0;
		}
		final double maxDist = 5.0;
		if (dist <= maxDist) {
			return 1.0;
		}
		if (dist > maxDist + 3.0) {
			return 0.0;
		}
		return (maxDist + 3.0 - dist) / (maxDist - 3.0);
	}

}