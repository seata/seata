// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: abstractResultMessage.proto

package io.seata.core.protocol.protobuf;

public final class AbstractResultMessage {
  private AbstractResultMessage() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_seata_protocol_protobuf_AbstractResultMessageProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_seata_protocol_protobuf_AbstractResultMessageProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\033abstractResultMessage.proto\022\032io.seata." +
      "protocol.protobuf\032\020resultCode.proto\032\025abs" +
      "tractMessage.proto\"\311\001\n\032AbstractResultMes" +
      "sageProto\022I\n\017AbstractMessage\030\001 \001(\01320.io." +
      "seata.protocol.protobuf.AbstractMessageP" +
      "roto\022?\n\nresultCode\030\002 \001(\0162+.io.seata.prot" +
      "ocol.protobuf.ResultCodeProto\022\013\n\003msg\030\003 \001" +
      "(\t\022\022\n\nidentified\030\004 \001(\010B:\n\037io.seata.core." +
      "protocol.protobufB\025AbstractResultMessage" +
      "P\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          io.seata.core.protocol.protobuf.ResultCode.getDescriptor(),
          io.seata.core.protocol.protobuf.AbstractMessage.getDescriptor(),
        }, assigner);
    internal_static_io_seata_protocol_protobuf_AbstractResultMessageProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_seata_protocol_protobuf_AbstractResultMessageProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_seata_protocol_protobuf_AbstractResultMessageProto_descriptor,
        new java.lang.String[] { "AbstractMessage", "ResultCode", "Msg", "Identified", });
    io.seata.core.protocol.protobuf.ResultCode.getDescriptor();
    io.seata.core.protocol.protobuf.AbstractMessage.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
