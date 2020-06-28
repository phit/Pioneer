package ttftcuts.pioneer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import ttftcuts.pioneer.Pioneer;
import ttftcuts.pioneer.map.MapJob;

public class CommandPioneer {
    public CommandPioneer(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(Commands.literal("pioneer").requires(cs -> cs.getEntity() instanceof ServerPlayerEntity)
            .then(Commands.argument("radius", IntegerArgumentType.integer())
                .then(Commands.argument("scale", IntegerArgumentType.integer())
                    .then(Commands.argument("startcoords", Vec2Argument.vec2())
                        .executes(ctx -> {
                            Pioneer.logger.info("Pioneer: new mapping job?");
                            return doCommand(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "scale"),
                                Vec2Argument.getVec2f(ctx, "startcoords").x,
                                Vec2Argument.getVec2f(ctx, "startcoords").y);
                        })
                    )
                    .executes(ctx -> {
                        Pioneer.logger.info("Pioneer: new mapping job?");
                        return doCommand(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"),
                            IntegerArgumentType.getInteger(ctx, "scale"),
                            ctx.getSource().getPos().getX(),
                            ctx.getSource().getPos().getY());
                    })
                )
            )
            .then(
                Commands.literal("stop")
                    .executes(ctx -> {
                        if (Pioneer.currentJob != null) {
                            Pioneer.currentJob.endJob(true);
                            ctx.getSource().sendFeedback(new TranslationTextComponent("commands.pioneer.stop"), true);
                        } else {
                            throw new CommandException(new TranslationTextComponent("commands.pioneer.nojob"));
                        }
                        return 1;
                    })
            )
        );
    }


    private static int doCommand(CommandSource sender, int radius, int scale, double x, double z) throws CommandException {
        if (Pioneer.currentJob != null) {
            throw new CommandException(new TranslationTextComponent("commands.pioneer.busy"));
        }
        if (radius < 1 || scale < 1) {
            throw new CommandException(new TranslationTextComponent("commands.pioneer.positive"));
        }

        Pioneer.currentJob = new MapJob(sender.getWorld(), (int) x, (int) z, radius, scale);
        return 0;
    }
}
