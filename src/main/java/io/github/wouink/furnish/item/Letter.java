package io.github.wouink.furnish.item;

import io.github.wouink.furnish.Furnish;
import io.github.wouink.furnish.FurnishManager;
import io.github.wouink.furnish.client.gui.LetterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class Letter extends Item {
	public Letter(Properties p, String registryName) {
		super(p);
		setRegistryName(Furnish.MODID, registryName);
	}

	@OnlyIn(Dist.CLIENT)
	private void openGui(ItemStack stack, PlayerEntity playerEntity, Hand hand) {
		Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f));
		Minecraft.getInstance().setScreen(new LetterScreen(stack, playerEntity, hand));
	}

	@Override
	public void appendHoverText(ItemStack letter, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag tooltipFlag) {
		if(letter.hasTag()) {
			CompoundNBT letterTag = letter.getTag();
			if(letterTag.contains("Author")) {
				tooltip.add(new TranslationTextComponent("tooltip.furnish.letter.author", letterTag.getString("Author")).withStyle(TextFormatting.GRAY));
			}
			if(letterTag.contains("Attachment")) {
				ItemStack attachment = ItemStack.of(letterTag.getCompound("Attachment"));
				tooltip.add(new TranslationTextComponent("tooltip.furnish.letter.attachment", attachment.getItem().getDescription()).withStyle(TextFormatting.GRAY));
			}
		}
	}

	public static ItemStack addAttachment(ItemStack letter, ItemStack attachment) {
		CompoundNBT letterTag = letter.getOrCreateTag();
		if(!letterTag.contains("Attachment")) {
			CompoundNBT attachmentTag = new CompoundNBT();
			letterTag.put("Attachment", attachment.save(attachmentTag));
			letter.setTag(letterTag);
			return ItemStack.EMPTY;
		}
		return attachment;
	}

	public static ItemStack removeAttachment(ItemStack letter) {
		CompoundNBT letterTag = letter.getOrCreateTag();
		if(letterTag.contains("Attachment")) {
			ItemStack attachment = ItemStack.of(letterTag.getCompound("Attachment"));
			letterTag.remove("Attachment");
			letter.setTag(letterTag);
			return attachment;
		}
		return ItemStack.EMPTY;
	}

	public static void signLetter(ItemStack letter, String author) {
		CompoundNBT letterTag = letter.getOrCreateTag();
		if(!letterTag.contains("Author")) letterTag.putString("Author", author);
		letter.setTag(letterTag);
	}

	public static boolean canEditLetter(ItemStack letter) {
		CompoundNBT letterTag = letter.getOrCreateTag();
		if(letterTag.contains("Author")) return false;
		return true;
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
		ItemStack letter = playerEntity.getItemInHand(hand);
		if(playerEntity.isCrouching()) {
			if(hand != Hand.OFF_HAND) {
				ItemStack result = removeAttachment(letter);
				if(!result.isEmpty()) {
					playerEntity.addItem(result);
					if(world.isClientSide()) {
						playerEntity.displayClientMessage(new TranslationTextComponent("msg.furnish.letter.attachment_removed"), true);
						playerEntity.playSound(FurnishManager.Sounds.Detach_From_Letter.get(), 1.0f, 1.0f);
					}
					return ActionResult.sidedSuccess(letter, world.isClientSide());
				}
				ItemStack offHandStack = playerEntity.getItemInHand(Hand.OFF_HAND);
				if(!offHandStack.isEmpty()) {
					result = addAttachment(letter, offHandStack);
					playerEntity.setItemInHand(Hand.OFF_HAND, result);
					if(result.isEmpty()) {
						if(world.isClientSide()) {
							playerEntity.displayClientMessage(new TranslationTextComponent("msg.furnish.letter.attachment_added", offHandStack.getItem().getDescription()), true);
							playerEntity.playSound(FurnishManager.Sounds.Attach_To_Letter.get(), 1.0f, 1.0f);
						}
						return ActionResult.sidedSuccess(letter, world.isClientSide());
					}
				}
				return ActionResult.fail(letter);
			}
			return ActionResult.fail(letter);
		} else {
			if(world.isClientSide()) openGui(letter, playerEntity, hand);
			return ActionResult.sidedSuccess(letter, world.isClientSide());
		}
	}
}
