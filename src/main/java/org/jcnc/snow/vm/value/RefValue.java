package org.jcnc.snow.vm.value;

/**
 * A reference to a heap object managed by the Snow runtime.
 */
public record RefValue(int objectId) implements Value {
}