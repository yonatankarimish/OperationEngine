package com.SixSense.util;

public class DynamicFieldGlossary {
    /*Tracks all dynamic fields that are provided by the OperationEngine module
    * Dynamic field format is: public static final name_of_field = "name.of.field"
    * This helps us modify common dynamic fields in a central location*/

    public static final String device_host = "device.host";
    public static final String device_internal_id = "device.internal.id";
    public static final String device_password = "device.password";
    public static final String device_port = "device.port";
    public static final String device_username = "device.username";
    public static final String var_block_counter = "var.block.counter";
    public static final String var_block_id = "var.block.id";
    public static final String var_block_repeatCount = "var.block.repeatCount";
    public static final String var_cmd_text = "var.cmd.text";
    public static final String var_command_id = "var.command.id";
    public static final String var_operation_name = "var.operation.name";
    public static final String var_operation_product = "var.operation.product";
    public static final String var_operation_vendor = "var.operation.vendor";
    public static final String var_operation_version = "var.operation.version";
    public static final String var_scp_destination = "var.scp.destination";
    public static final String var_scp_source = "var.scp.source";
    public static final String var_scp_source_file_name = "var.scp.source_file_name";
    public static final String var_stack_text = "var.stack.text";

    public static String $(String variable){
        return MessageLiterals.VariableMark + variable;
    }
}
