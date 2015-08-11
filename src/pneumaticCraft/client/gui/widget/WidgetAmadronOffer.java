package pneumaticCraft.client.gui.widget;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import pneumaticCraft.common.recipes.AmadronOffer;
import pneumaticCraft.lib.Textures;

public class WidgetAmadronOffer extends WidgetBase{
    private final AmadronOffer offer;
    private final List<IGuiWidget> widgets = new ArrayList<IGuiWidget>();
    private int shoppingAmount;
    private boolean canBuy;
    private final Rectangle[] tooltipRectangles = new Rectangle[2];
    private boolean renderBackground = true;

    public WidgetAmadronOffer(int id, int x, int y, AmadronOffer offer){
        super(id, x, y, 73, 35);
        this.offer = offer;
        if(offer.getInput() instanceof FluidStack) {
            widgets.add(new WidgetFluidStack(0, x + 6, y + 15, (FluidStack)offer.getInput()));
        }
        if(offer.getOutput() instanceof FluidStack) {
            widgets.add(new WidgetFluidStack(0, x + 51, y + 15, (FluidStack)offer.getOutput()));
        }
        tooltipRectangles[0] = new Rectangle(x + 6, y + 15, 16, 16);
        tooltipRectangles[1] = new Rectangle(x + 51, y + 15, 16, 16);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick){
        GL11.glDisable(GL11.GL_LIGHTING);
        if(renderBackground) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(Textures.WIDGET_AMADRON_OFFER);
            GL11.glColor4d(1, canBuy ? 1 : 0.4, canBuy ? 1 : 0.4, 1);
            Gui.func_146110_a(x, y, 0, 0, getBounds().width, getBounds().height, 256, 256);
        }
        for(IGuiWidget widget : widgets) {
            widget.render(mouseX, mouseY, partialTick);
        }
        Minecraft.getMinecraft().fontRenderer.drawString(offer.getVendor(), x + 2, y + 2, 0xFF000000);
        if(shoppingAmount > 0) {
            Minecraft.getMinecraft().fontRenderer.drawString(shoppingAmount + "", x + 36 - Minecraft.getMinecraft().fontRenderer.getStringWidth("" + shoppingAmount) / 2, y + 20, 0xFF000000);

        }
    }

    public WidgetAmadronOffer setDrawBackground(boolean drawBackground){
        renderBackground = drawBackground;
        return this;
    }

    public void setCanBuy(boolean canBuy){
        this.canBuy = canBuy;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTip, boolean shiftPressed){
        super.addTooltip(mouseX, mouseY, curTip, shiftPressed);
        for(IGuiWidget widget : widgets) {
            if(widget.getBounds().contains(mouseX, mouseY)) {

                widget.addTooltip(mouseX, mouseY, curTip, shiftPressed);
            }
        }
        boolean isInBounds = false;
        for(Rectangle rect : tooltipRectangles) {
            if(rect.contains(mouseX, mouseY)) {
                isInBounds = true;
            }
        }
        if(!isInBounds) {
            curTip.add(I18n.format("gui.amadron.amadronWidget.selling", getStringForObject(offer.getOutput())));
            curTip.add(I18n.format("gui.amadron.amadronWidget.buying", getStringForObject(offer.getInput())));
            curTip.add(I18n.format("gui.amadron.amadronWidget.vendor", offer.getVendor()));
            curTip.add(I18n.format("gui.amadron.amadronWidget.inBasket", shoppingAmount));
        }
    }

    private String getStringForObject(Object o){
        if(o instanceof ItemStack) {
            ItemStack stack = (ItemStack)o;
            return stack.stackSize + "x " + stack.getDisplayName();
        } else {
            FluidStack stack = (FluidStack)o;
            return stack.amount + "mB " + stack.getLocalizedName();
        }
    }

    public AmadronOffer getOffer(){
        return offer;
    }

    public void setShoppingAmount(int amount){
        shoppingAmount = amount;
    }
}
