package org.jcnc.snow.vm.runtime;

/**
 * HeapObject is a Snow heap-allocated runtime object.
 * It must not expose host containers across ABI boundaries.
 */
public sealed interface HeapObject permits
        SnowStringObject,
        SnowBytesObject,
        SnowArrayObject,
        SnowDictObject {

    HeapObjectKind kind();
}