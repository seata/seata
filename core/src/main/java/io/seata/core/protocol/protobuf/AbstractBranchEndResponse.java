// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: abstractBranchEndResponse.proto

package io.seata.core.protocol.protobuf;

public final class AbstractBranchEndResponse {
  private AbstractBranchEndResponse() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_seata_protocol_protobuf_AbstractBranchEndResponseProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_seata_protocol_protobuf_AbstractBranchEndResponseProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\037abstractBranchEndResponse.proto\022\032io.se" +
      "ata.protocol.protobuf\032!abstractTransacti" +
      "onResponse.proto\032\022branchStatus.proto\"\347\001\n" +
      "\036AbstractBranchEndResponseProto\022a\n\033abstr" +
      "actTransactionResponse\030\001 \001(\0132<.io.seata." +
      "protocol.protobuf.AbstractTransactionRes" +
      "ponseProto\022\013\n\003xid\030\002 \001(\t\022\020\n\010branchId\030\003 \001(" +
      "\003\022C\n\014branchStatus\030\004 \001(\0162-.io.seata.proto" +
      "col.protobuf.BranchStatusProtoB>\n\037io.sea" +
      "ta.core.protocol.protobufB\031AbstractBranc" +
      "hEndResponseP\001b\006proto3"
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
          io.seata.core.protocol.protobuf.AbstractTransactionResponse.getDescriptor(),
          io.seata.core.protocol.protobuf.BranchStatus.getDescriptor(),
        }, assigner);
    internal_static_io_seata_protocol_protobuf_AbstractBranchEndResponseProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_seata_protocol_protobuf_AbstractBranchEndResponseProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_seata_protocol_protobuf_AbstractBranchEndResponseProto_descriptor,
        new java.lang.String[] { "AbstractTransactionResponse", "Xid", "BranchId", "BranchStatus", });
    io.seata.core.protocol.protobuf.AbstractTransactionResponse.getDescriptor();
    io.seata.core.protocol.protobuf.BranchStatus.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
