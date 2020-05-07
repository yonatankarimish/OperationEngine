package com.sixsense.model.exceptions;


/*Each exception has a reference id, assigned from an externally managed range.
* For all purposes, the reference ids are unique under the operation engine's source code*/
public enum EngineExceptionType {

    /*Initialization and teardown related exceptions*/
    ENGINE_INITIALIZATION_EXCEPTION (11001),
    ENGINE_CONFIGURATION_EXCEPTION (11002),
    CLOSABLE_RESOURCE_EXCEPTION (11003),

    /*Local shell exceptions*/
    LOCAL_SHELL_EXCEPTION (11051),

    /*Serialization related exceptions (JSON, file, toString() etc...)*/
    SERIALIZATION_EXCEPTION (11061),
    DESERIALIZATION_EXCEPTION (11062),

    /*File I/O related exceptions*/
    FILE_WRITE_EXCEPTION (11081),
    FILE_READ_EXCEPTION (11082),

    /*HTTP related exceptions*/
    HTTP_EXCEPTION (11101),

    /*AMQP related exceptions*/
    AMQP_EXCEPTION (11201),
    AMQP_ROUTING_EXCEPTION (11202),
    AMQP_EXCHANGE_EXCEPTION (11203),
    AMQP_PRODUCE_EXCEPTION (11204),
    AMQP_CONSUME_EXCEPTION (11205),
    AMQP_ACKNOWLEDGEMENT_EXCEPTION (11206),

    /*Threading manager related exceptions*/
    OPERATION_SUBMISSION_EXCEPTION (11301),
    WORKFLOW_SUBMISSION_EXCEPTION (11302),

    /*Session engine related exceptions*/
    COMMAND_EXECUTION_EXCEPTION (11303),
    BLOCK_EXECUTION_EXCEPTION (11304),
    OPERATION_EXECUTION_EXCEPTION (11305),
    WORKFLOW_EXECUTION_EXCEPTION (11306),

    /*Event emission and propagation exceptions*/
    EVENT_EMISSION_EXCEPTION (11401),

    /*Session I/O related exceptions*/
    CHANNEL_WRITE_EXCEPTION (11451),
    CHANNEL_READ_EXCEPTION (11452),
    PSW_WRITE_EXCEPTION (11453),
    PSW_READ_EXCEPTION (11454),

    /*Result related exceptions*/
    RESULT_PROCESSING_EXCEPTION (11501),
    RESULT_RETENTION_EXCEPTION (11502);

    private static final String PREFIX = "engine-";
    private final int exceptionId;

    EngineExceptionType(int exceptionId){
        this.exceptionId = exceptionId;
    }

    public int getExceptionId() {
        return this.exceptionId;
    }

    public String getPrefixAndCode() {
        return PREFIX + this.exceptionId;
    }
}
