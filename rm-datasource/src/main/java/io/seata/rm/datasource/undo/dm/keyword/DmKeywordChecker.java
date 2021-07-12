/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo.dm.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type dm sql keyword checker.
 *
 * @author chengxiaoxiao
 */
@LoadLevel(name = JdbcConstants.DM)
public class DmKeywordChecker implements KeywordChecker {

    private Set<String> keywordSet = Arrays.stream(DmKeyword.values()).map(DmKeyword::name).collect(Collectors.toSet());

    /**
     * dm keyword
     */
    private enum DmKeyword {

        /**
         * ABORT
         */
        ABORT("ABORT"),
        ABSOLUTE("ABSOLUTE"),
        ABSTRACT("ABSTRACT"),
        ACCESSED("ACCESSED"),
        ACCOUNT("ACCOUNT"),
        ACROSS("ACROSS"),
        ACTION("ACTION"),
        ADD("ADD"),

        ADMIN("ADMIN"),
        AFTER("AFTER"),
        AGGREGATE("AGGREGATE"),
        ALL("ALL"),
        ALLOW_DATETIME("ALLOW_DATETIME"),
        ALLOW_IP("ALLOW_IP"),
        ALTER("ALTER"),
        ANALYZE("ANALYZE"),

        AND("AND"),
        ANY("ANY"),
        ARCHIVE("ARCHIVE"),
        ARCHIVEDIR("ARCHIVEDIR"),
        ARCHIVELOG("ARCHIVELOG"),
        ARCHIVESTYLE("ARCHIVESTYLE"),
        ARRAY("ARRAY"),

        ARRAYLEN("ARRAYLEN"),
        AS("AS"),
        ASC("ASC"),
        ASENSITIVE("ASENSITIVE"),
        ASSIGN("ASSIGN"),
        ASYNCHRONOUS("ASYNCHRONOUS"),
        AT("AT"),
        ATTACH("ATTACH"),

        AUDIT("AUDIT"),
        AUTHID("AUTHID"),
        AUTHORIZATION("AUTHORIZATION"),
        AUTO("AUTO"),
        AUTOEXTEND("AUTOEXTEND"),

        AUTONOMOUS_TRANSACTION("AUTONOMOUS_TRANSACTION"),
        AVG("AVG"),

        BACKED("BACKED"),
        BACKUP("BACKUP"),
        BACKUPDIR("BACKUPDIR"),
        BACKUPINFO("BACKUPINFO"),
        BACKSET("BACKSET"),
        BADFILE("BADFILE"),
        BAKFILE("BAKFILE"),

        BASE("BASE"),
        BEFORE("BEFORE"),
        BEGIN("BEGIN"),
        BETWEEN("BETWEEN"),
        BIGDATEDIFF("BIGDATEDIFF"),
        BIGINT("BIGINT"),
        BINARY("BINARY"),
        BIT("BIT"),
        BITMAP("BITMAP"),

        BLOB("BLOB"),
        BLOCK("BLOCK"),
        BOOL("BOOL"),
        BOOLEAN("BOOLEAN"),
        BOTH("BOTH"),
        BRANCH("BRANCH"),
        BREAK("BREAK"),
        BSTRING("BSTRING"),
        BTREE("BTREE"),

        BUFFER("BUFFER"),
        BUILD("BUILD"),
        BULK("BULK"),
        BY("BY"),
        BYTE("BYTE"),
        C("C"),
        CACHE("CACHE"),
        CALCULATE("CALCULATE"),
        CALL("CALL"),
        CASCADE("CASCADE"),
        CASCADED("CASCADED"),
        CASE("CASE"),
        CAST("CAST"),
        CATALOG("CATALOG"),

        CATCH("CATCH"),
        CHAIN("CHAIN"),
        CHAR("CHAR"),
        CHARACTER("CHARACTER"),
        CHARACTERISTICS("CHARACTERISTICS"),
        CHECK("CHECK"),
        CIPHER("CIPHER"),
        CLASS("CLASS"),

        CLOB("CLOB"),
        CLOSE("CLOSE"),
        CLUSTER("CLUSTER"),
        CLUSTERBTR("CLUSTERBTR"),
        COLLATE("COLLATE"),
        COLLATION("COLLATION"),
        COLLECT("COLLECT"),
        COLUMN("COLUMN"),

        COLUMNS("COLUMNS"),
        COMMENT("COMMENT"),
        COMMIT("COMMIT"),
        COMMITTED("COMMITTED"),
        COMMITWORK("COMMITWORK"),
        COMPILE("COMPILE"),
        COMPLETE("COMPLETE"),

        COMPRESS("COMPRESS"),
        COMPRESSED("COMPRESSED"),
        CONNECT("CONNECT"),
        CONNECT_BY_IS_CYCLE("CONNECT_BY_IS_CYCLE"),

        CONNECT_BY_ISLEAF("CONNECT_BY_ISLEAF"),
        CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
        CONNECT_IDLE_TIME("CONNECT_IDLE_TIME"),
        CONNECT_TIME("CONNECT_TIME"),

        CONST("CONST"),
        CONSTANT("CONSTANT"),
        CONSTRAINT("CONSTRAINT"),
        CONSTRAINTS("CONSTRAINTS"),
        CONSTRUCTOR("CONSTRUCTOR"),
        CONTAINS("CONTAINS"),

        CONTEXT("CONTEXT"),
        CONTINUE("CONTINUE"),
        CONVERT("CONVERT"),
        COPY("COPY"),
        CORRESPONDING("CORRESPONDING"),
        COUNT("COUNT"),
        COUNTER("COUNTER"),

        CPU_PER_CALL("CPU_PER_CALL"),
        CPU_PER_SESSION("CPU_PER_SESSION"),
        CREATE("CREATE"),
        CROSS("CROSS"),
        CRYPTO("CRYPTO"),
        CTLFILE("CTLFILE"),
        CUBE("CUBE"),

        CUMULATIVE("CUMULATIVE"),
        CURRENT("CURRENT"),
        CURRENT_SCHEMA("CURRENT_SCHEMA"),
        CURRENT_USER("CURRENT_USER"),
        CURSOR("CURSOR"),
        CYCLE("CYCLE"),
        D("D"),
        DANGLING("DANGLING"),
        DATA("DATA"),
        DATABASE("DATABASE"),
        DATAFILE("DATAFILE"),
        DATE("DATE"),
        DATEADD("DATEADD"),
        DATEDIFF("DATEDIFF"),

        DATEPART("DATEPART"),
        DATETIME("DATETIME"),
        DAY("DAY"),
        DBFILE("DBFILE"),
        DDL("DDL"),
        DDL_CLONE("DDL_CLONE"),
        DEBUG("DEBUG"),
        DEC("DEC"),
        DECIMAL("DECIMAL"),

        DECLARE("DECLARE"),
        DECODE("DECODE"),
        DEFAULT("DEFAULT"),
        DEFERRABLE("DEFERRABLE"),
        DEFERRED("DEFERRED"),
        DEFINER("DEFINER"),
        DELETE("DELETE"),

        DELETING("DELETING"),
        DELIMITED("DELIMITED"),
        DELTA("DELTA"),
        DEMAND("DEMAND"),
        DENSE_RANK("DENSE_RANK"),
        DEREF("DEREF"),
        DESC("DESC"),
        DETACH("DETACH"),

        DETERMINISTIC("DETERMINISTIC"),
        DEVICE("DEVICE"),
        DIAGNOSTICS("DIAGNOSTICS"),
        DICTIONARY("DICTIONARY"),
        DISABLE("DISABLE"),
        DISCONNECT("DISCONNECT"),

        DISKSPACE("DISKSPACE"),
        DISTINCT("DISTINCT"),
        DISTRIBUTED("DISTRIBUTED"),
        DO("DO"),
        DOMAIN("DOMAIN"),
        DOUBLE("DOUBLE"),
        DOWN("DOWN"),
        DROP("DROP"),

        DUMP("DUMP"),
        E("E"),
        EACH("EACH"),
        ELSE("ELSE"),
        ELSEIF("ELSEIF"),
        ELSIF("ELSIF"),
        ENABLE("ENABLE"),
        ENCRYPT("ENCRYPT"),
        ENCRYPTION("ENCRYPTION"),
        END("END"),

        EQU("EQU"),
        ERROR("ERROR"),
        ERRORS("ERRORS"),
        ESCAPE("ESCAPE"),
        EVENTINFO("EVENTINFO"),
        EVENTS("EVENTS"),
        EXCEPT("EXCEPT"),
        EXCEPTION("EXCEPTION"),

        EXCEPTIONS("EXCEPTIONS"),
        EXCEPTION_INIT("EXCEPTION_INIT"),
        EXCHANGE("EXCHANGE"),
        EXCLUDE("EXCLUDE"),
        EXCLUDING("EXCLUDING"),
        EXCLUSIVE("EXCLUSIVE"),

        EXEC("EXEC"),
        EXECUTE("EXECUTE"),
        EXISTS("EXISTS"),
        EXIT("EXIT"),
        EXPLAIN("EXPLAIN"),
        EXTENDS("EXTENDS"),
        EXTERN("EXTERN"),
        EXTERNAL("EXTERNAL"),

        EXTERNALLY("EXTERNALLY"),
        EXTRACT("EXTRACT"),
        F("F"),
        FAILED_LOGIN_ATTEMPS("FAILED_LOGIN_ATTEMPS"),
        FAST("FAST"),
        FETCH("FETCH"),
        FIELDS("FIELDS"),
        FILE("FILE"),
        FILEGROUP("FILEGROUP"),

        FILESIZE("FILESIZE"),
        FILLFACTOR("FILLFACTOR"),
        FINAL("FINAL"),
        FINALLY("FINALLY"),
        FIRST("FIRST"),
        FLOAT("FLOAT"),
        FOLLOWING("FOLLOWING"),
        FOR("FOR"),

        FORALL("FORALL"),
        FORCE("FORCE"),
        FOREIGN("FOREIGN"),
        FREQUENCE("FREQUENCE"),
        FROM("FROM"),
        FULL("FULL"),
        FULLY("FULLY"),
        FUNCTION("FUNCTION"),

        GET("GET"),
        GLOBAL("GLOBAL"),
        GLOBALLY("GLOBALLY"),
        GOTO("GOTO"),
        GRANT("GRANT"),
        GROUP("GROUP"),
        GROUPING("GROUPING"),

        HASH("HASH"),
        HAVING("HAVING"),
        HEXTORAW("HEXTORAW"),
        HOLD("HOLD"),
        HOUR("HOUR"),
        HUGE("HUGE"),

        IDENTIFIED("IDENTIFIED"),
        IDENTITY("IDENTITY"),
        IDENTITY_INSERT("IDENTITY_INSERT"),
        IF("IF"),
        IMAGE("IMAGE"),
        IMMEDIATE("IMMEDIATE"),

        IN("IN"),
        INCLUDE("INCLUDE"),
        INCLUDING("INCLUDING"),
        INCREASE("INCREASE"),
        INCREMENT("INCREMENT"),
        INDEX("INDEX"),
        INDEXES("INDEXES"),
        INDICES("INDICES"),

        INITIAL("INITIAL"),
        INITIALIZED("INITIALIZED"),
        INITIALLY("INITIALLY"),
        INLINE("INLINE"),
        INNER("INNER"),
        INNERID("INNERID"),
        INPUT("INPUT"),

        INSENSITIVE("INSENSITIVE"),
        INSERT("INSERT"),
        INSERTING("INSERTING"),
        INSTANTIABLE("INSTANTIABLE"),
        INSTEAD("INSTEAD"),
        INT("INT"),
        INTEGER("INTEGER"),

        INTENT("INTENT"),
        INTERNAL("INTERNAL"),
        INTERSECT("INTERSECT"),
        INTERVAL("INTERVAL"),
        INTO("INTO"),
        INVISIBLE("INVISIBLE"),
        IS("IS"),
        ISOLATION("ISOLATION"),

        JAVA("JAVA"),
        JOB("JOB"),
        JOIN("JOIN"),

        KEEP("KEEP"),
        KEY("KEY"),

        LABEL("LABEL"),
        LARGE("LARGE"),
        LAST("LAST"),
        LEADING("LEADING"),
        LEFT("LEFT"),
        LESS("LESS"),
        LEVEL("LEVEL"),
        LEXER("LEXER"),
        LIKE("LIKE"),
        LIMIT("LIMIT"),

        LINK("LINK"),
        LIST("LIST"),
        LNNVL("LNNVL"),
        LOB("LOB"),
        LOCAL("LOCAL"),
        LOCALLY("LOCALLY"),
        LOCK("LOCK"),
        LOCKED("LOCKED"),
        LOG("LOG"),
        LOGFILE("LOGFILE"),

        LOGGING("LOGGING"),
        LOGIN("LOGIN"),
        LOGOFF("LOGOFF"),
        LOGON("LOGON"),
        LOGOUT("LOGOUT"),
        LONG("LONG"),
        LONGVARBINARY("LONGVARBINARY"),
        LONGVARCHAR("LONGVARCHAR"),

        LOOP("LOOP"),
        LSN("LSN"),

        MANUAL("MANUAL"),
        MAP("MAP"),
        MAPPED("MAPPED"),
        MATCH("MATCH"),
        MATCHED("MATCHED"),
        MATERIALIZED("MATERIALIZED"),
        MAX("MAX"),
        MAXPIECESIZE("MAXPIECESIZE"),

        MAXSIZE("MAXSIZE"),
        MAXVALUE("MAXVALUE"),
        MEMBER("MEMBER"),
        MEMORY("MEMORY"),
        MEM_SPACE("MEM_SPACE"),
        MERGE("MERGE"),
        MIN("MIN"),
        MINEXTENTS("MINEXTENTS"),

        MINUS("MINUS"),
        MINUTE("MINUTE"),
        MINVALUE("MINVALUE"),
        MIRROR("MIRROR"),
        MOD("MOD"),
        MODE("MODE"),
        MODIFY("MODIFY"),
        MONEY("MONEY"),
        MONITORING("MONITORING"),

        MONTH("MONTH"),
        MOUNT("MOUNT"),
        MOVEMENT("MOVEMENT"),

        NATIONAL("NATIONAL"),
        NATURAL("NATURAL"),
        NCHAR("NCHAR"),
        NCHARACTER("NCHARACTER"),
        NEVER("NEVER"),
        NEW("NEW"),
        NEXT("NEXT"),
        NO("NO"),

        NOARCHIVELOG("NOARCHIVELOG"),
        NOAUDIT("NOAUDIT"),
        NOBRANCH("NOBRANCH"),
        NOCACHE("NOCACHE"),
        NOCOPY("NOCOPY"),
        NOCYCLE("NOCYCLE"),
        NOLOGGING("NOLOGGING"),

        NOMAXVALUE("NOMAXVALUE"),
        NOMINVALUE("NOMINVALUE"),
        NOMONITORING("NOMONITORING"),
        NONE("NONE"),
        NOORDER("NOORDER"),
        NORMAL("NORMAL"),
        NOSORT("NOSORT"),

        NOT("NOT"),
        NOT_ALLOW_DATETIME("NOT_ALLOW_DATETIME"),
        NOT_ALLOW_IP("NOT_ALLOW_IP"),
        NOWAIT("NOWAIT"),
        NULL("NULL"),
        NULLS("NULLS"),
        NUMBER("NUMBER"),

        NUMERIC("NUMERIC"),

        OBJECT("OBJECT"),
        OF("OF"),
        OFF("OFF"),
        OFFLINE("OFFLINE"),
        OFFSET("OFFSET"),
        OLD("OLD"),
        ON("ON"),
        ONCE("ONCE"),
        ONLINE("ONLINE"),
        ONLY("ONLY"),

        OPEN("OPEN"),
        OPTIMIZE("OPTIMIZE"),
        OPTION("OPTION"),
        OR("OR"),
        ORDER("ORDER"),
        OUT("OUT"),
        OUTER("OUTER"),
        OVER("OVER"),
        OVERLAPS("OVERLAPS"),

        OVERLAY("OVERLAY"),
        OVERRIDE("OVERRIDE"),
        OVERRIDING("OVERRIDING"),

        PACKAGE("PACKAGE"),
        PAD("PAD"),
        PAGE("PAGE"),
        PARALLEL("PARALLEL"),
        PARALLEL_ENABLE("PARALLEL_ENABLE"),
        PARMS("PARMS"),
        PARTIAL("PARTIAL"),

        PARTITION("PARTITION"),
        PARTITIONS("PARTITIONS"),
        PASSWORD_GRACE_TIME("PASSWORD_GRACE_TIME"),
        PASSWORD_LIFE_TIME("PASSWORD_LIFE_TIME"),


        PASSWORD_LOCK_TIME("PASSWORD_LOCK_TIME"),
        PASSWORD_POLICY("PASSWORD_POLICY"),
        PASSWORD_REUSE_MAX("PASSWORD_REUSE_MAX"),

        PASSWORD_REUSE_TIME("PASSWORD_REUSE_TIME"),
        PATH("PATH"),
        PENDANT("PENDANT"),
        PERCENT("PERCENT"),
        PIPE("PIPE"),
        PIPELINED("PIPELINED"),
        PIVOT("PIVOT"),

        PLACING("PLACING"),
        PLS_INTEGER("PLS_INTEGER"),
        PRAGMA("PRAGMA"),
        PRECEDING("PRECEDING"),
        PRECISION("PRECISION"),
        PRESERVE("PRESERVE"),
        PRIMARY("PRIMARY"),

        PRINT("PRINT"),
        PRIOR("PRIOR"),
        PRIVATE("PRIVATE"),
        PRIVILEGE("PRIVILEGE"),
        PRIVILEGES("PRIVILEGES"),
        PROCEDURE("PROCEDURE"),
        PROTECTED("PROTECTED"),

        PUBLIC("PUBLIC"),
        PURGE("PURGE"),

        QUERY_REWRITE_INTEGRITY("QUERY_REWRITE_INTEGRITY"),

        RAISE("RAISE"),
        RANDOMLY("RANDOMLY"),
        RANGE("RANGE"),
        RAWTOHEX("RAWTOHEX"),
        READ("READ"),
        READONLY("READONLY"),
        READ_PER_CALL("READ_PER_CALL"),

        READ_PER_SESSION("READ_PER_SESSION"),
        REAL("REAL"),
        REBUILD("REBUILD"),
        RECORD("RECORD"),
        RECORDS("RECORDS"),
        REF("REF"),
        REFERENCE("REFERENCE"),

        REFERENCES("REFERENCES"),
        REFERENCING("REFERENCING"),
        REFRESH("REFRESH"),
        RELATED("RELATED"),
        RELATIVE("RELATIVE"),
        RENAME("RENAME"),
        REPEAT("REPEAT"),

        REPEATABLE("REPEATABLE"),
        REPLACE("REPLACE"),
        REPLAY("REPLAY"),
        REPLICATE("REPLICATE"),
        RESIZE("RESIZE"),
        RESTORE("RESTORE"),
        RESTRICT("RESTRICT"),

        RESULT("RESULT"),
        RESULT_CACHE("RESULT_CACHE"),
        RETURN("RETURN"),
        RETURNING("RETURNING"),
        REVERSE("REVERSE"),
        REVOKE("REVOKE"),
        RIGHT("RIGHT"),

        ROLE("ROLE"),
        ROLLBACK("ROLLBACK"),
        ROLLFILE("ROLLFILE"),
        ROLLUP("ROLLUP"),
        ROOT("ROOT"),
        ROW("ROW"),
        ROWCOUNT("ROWCOUNT"),
        ROWID("ROWID"),
        ROWNUM("ROWNUM"),

        ROWS("ROWS"),
        RULE("RULE"),

        SALT("SALT"),
        SAMPLE("SAMPLE"),
        SAVE("SAVE"),
        SAVEPOINT("SAVEPOINT"),
        SBYTE("SBYTE"),
        SCHEMA("SCHEMA"),
        SCOPE("SCOPE"),
        SCROLL("SCROLL"),
        SEALED("SEALED"),

        SECOND("SECOND"),
        SECTION("SECTION"),
        SEED("SEED"),
        SELECT("SELECT"),
        SELF("SELF"),
        SENSITIVE("SENSITIVE"),
        SEQUENCE("SEQUENCE"),
        SERERR("SERERR"),

        SERIALIZABLE("SERIALIZABLE"),
        SERVER("SERVER"),
        SESSION("SESSION"),
        SESSION_PER_USER("SESSION_PER_USER"),
        SET("SET"),
        SETS("SETS"),
        SHARE("SHARE"),

        SHORT("SHORT"),
        SHUTDOWN("SHUTDOWN"),
        SIBLINGS("SIBLINGS"),
        SIMPLE("SIMPLE"),
        SINCE("SINCE"),
        SIZE("SIZE"),
        SIZEOF("SIZEOF"),
        SKIP("SKIP"),
        SMALLINT("SMALLINT"),

        SNAPSHOT("SNAPSHOT"),
        SOME("SOME"),
        SOUND("SOUND"),
        SPACE("SPACE"),
        SPATIAL("SPATIAL"),
        SPFILE("SPFILE"),
        SPLIT("SPLIT"),
        SQL("SQL"),
        STANDBY("STANDBY"),

        STARTUP("STARTUP"),
        STAT("STAT"),
        STATEMENT("STATEMENT"),
        STATIC("STATIC"),
        STDDEV("STDDEV"),
        STORAGE("STORAGE"),
        STORE("STORE"),
        STRING("STRING"),

        STRUCT("STRUCT"),
        STYLE("STYLE"),
        SUBPARTITION("SUBPARTITION"),
        SUBPARTITIONS("SUBPARTITIONS"),
        SUBSTRING("SUBSTRING"),
        SUBTYPE("SUBTYPE"),

        SUCCESSFUL("SUCCESSFUL"),
        SUM("SUM"),
        SUSPEND("SUSPEND"),
        SWITCH("SWITCH"),
        SYNC("SYNC"),
        SYNCHRONOUS("SYNCHRONOUS"),
        SYNONYM("SYNONYM"),
        SYSTEM("SYSTEM"),

        SYS_CONNECT_BY_PATH("SYS_CONNECT_BY_PATH"),

        TABLE("TABLE"),
        TABLESPACE("TABLESPACE"),
        TASK("TASK"),
        TEMPLATE("TEMPLATE"),
        TEMPORARY("TEMPORARY"),
        TEXT("TEXT"),
        THAN("THAN"),
        THEN("THEN"),

        THREAD("THREAD"),
        THROW("THROW"),
        TIES("TIES"),
        TIME("TIME"),
        TIMER("TIMER"),
        TIMES("TIMES"),
        TIMESTAMP("TIMESTAMP"),
        TIMESTAMPADD("TIMESTAMPADD"),

        TIMESTAMPDIFF("TIMESTAMPDIFF"),
        TIME_ZONE("TIME_ZONE"),
        TINYINT("TINYINT"),
        TO("TO"),
        TOP("TOP"),
        TRACE("TRACE"),
        TRAILING("TRAILING"),

        TRANSACTION("TRANSACTION"),
        TRANSACTIONAL("TRANSACTIONAL"),
        TRIGGER("TRIGGER"),
        TRIGGERS("TRIGGERS"),
        TRIM("TRIM"),
        TRUNCATE("TRUNCATE"),

        TRUNCSIZE("TRUNCSIZE"),
        TRXID("TRXID"),
        TRY("TRY"),
        TYPE("TYPE"),
        TYPEDEF("TYPEDEF"),
        TYPEOF("TYPEOF"),

        UINT("UINT"),
        ULONG("ULONG"),
        UNBOUNDED("UNBOUNDED"),
        UNCOMMITTED("UNCOMMITTED"),
        UNDER("UNDER"),
        UNION("UNION"),
        UNIQUE("UNIQUE"),

        UNLIMITED("UNLIMITED"),
        UNLOCK("UNLOCK"),
        UNPIVOT("UNPIVOT"),
        UNTIL("UNTIL"),
        UNUSABLE("UNUSABLE"),
        UP("UP"),
        UPDATE("UPDATE"),
        UPDATING("UPDATING"),

        USAGE("USAGE"),
        USER("USER"),
        USE_HASH("USE_HASH"),
        USE_MERGE("USE_MERGE"),
        USE_NL("USE_NL"),
        USE_NL_WITH_INDEX("USE_NL_WITH_INDEX"),

        USHORT("USHORT"),
        USING("USING"),

        VALUE("VALUE"),
        VALUES("VALUES"),
        VARBINARY("VARBINARY"),
        VARCHAR("VARCHAR"),
        VARCHAR2("VARCHAR2"),
        VARIANCE("VARIANCE"),
        VARRAY("VARRAY"),

        VARYING("VARYING"),
        VERIFY("VERIFY"),
        VERSIONS("VERSIONS"),
        VERSIONS_STARTTIME("VERSIONS_STARTTIME"),
        VERSIONS_ENDTIME("VERSIONS_ENDTIME"),

        VERSIONS_STARTTRXID("VERSIONS_STARTTRXID"),
        VERSIONS_ENDTRXID("VERSIONS_ENDTRXID"),
        VERSIONS_OPERATION("VERSIONS_OPERATION"),
        VERTICAL("VERTICAL"),

        VIEW("VIEW"),
        VIRTUAL("VIRTUAL"),
        VISIBLE("VISIBLE"),
        VOID("VOID"),
        VOLATILE("VOLATILE"),
        VSIZE("VSIZE"),

        WAIT("WAIT"),
        WEEK("WEEK"),
        WHEN("WHEN"),
        WHENEVER("WHENEVER"),
        WHERE("WHERE"),
        WHILE("WHILE"),
        WITH("WITH"),
        WITHIN("WITHIN"),
        WITHOUT("WITHOUT"),

        WORK("WORK"),
        WRAPPED("WRAPPED"),
        WRITE("WRITE"),
        XML("XML"),
        YEAR("YEAR"),
        ZONE("ZONE");
        /**
         * The Name.
         */
        public final String name;

        DmKeyword(String name) {
            this.name = name;
        }
    }

    @Override
    public boolean check(String fieldOrTableName) {
        if (keywordSet.contains(fieldOrTableName)) {
            return true;
        }
        if (fieldOrTableName != null) {
            fieldOrTableName = fieldOrTableName.toUpperCase();
        }
        return keywordSet.contains(fieldOrTableName);

    }

    @Override
    public boolean checkEscape(String fieldOrTableName) {
        boolean check = check(fieldOrTableName);
        // dm
        // we are recommend table name and column name must uppercase.
        // if exists full uppercase, the table name or column name does't bundle escape symbol.
        if (!check && isUppercase(fieldOrTableName)) {
            return false;
        }
        return true;
    }

    private static boolean isUppercase(String fieldOrTableName) {
        if (fieldOrTableName == null) {
            return false;
        }
        char[] chars = fieldOrTableName.toCharArray();
        for (char ch : chars) {
            if (ch >= 'a' && ch <= 'z') {
                return false;
            }
        }
        return true;
    }
}
