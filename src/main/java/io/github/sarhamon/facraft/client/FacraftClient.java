package io.github.sarhamon.facraft.client;

import io.github.sarhamon.facraft.Facraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// 클라이언트에서만 로드됨. 전용 서버에서는 이 클래스를 읽지 않음
@EventBusSubscriber(modid = Facraft.MODID, value = Dist.CLIENT)
public class FacraftClient {
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Facraft.ASSEMBLING_MACHINE_MENU.get(), AssemblingMachineScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Facraft.TRANSPORT_BELT_BLOCK_ENTITY.get(), TransportBeltRenderer::new);
    }
}
