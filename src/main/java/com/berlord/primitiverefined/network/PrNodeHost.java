package com.berlord.primitiverefined.network;

/**
 * Implemented by every block entity in this mod that is a node on a primitive network.
 *
 * <p>It exists because Refined Storage's own helper base class,
 * {@code AbstractNetworkNodeContainerBlockEntity}, extends {@code BlockEntity} directly and
 * every block here already extends Create's {@code KineticBlockEntity}. Java has one
 * superclass to give and Create has it, so the RS half is composed instead of inherited:
 * each block entity owns a {@link PrNetworkNodeContainer} and hands it out through this
 * interface.
 *
 * <p>Nothing is lost by doing it this way. RS looks a node up through a NeoForge block
 * capability, not through an {@code instanceof}, so a container it never sees the class of
 * is a first-class member of a network.
 */
public interface PrNodeHost {

    PrNetworkNodeContainer prNode();
}
