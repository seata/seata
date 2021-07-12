CREATE TABLE "UNDO_LOG"
(
"ID" BIGINT IDENTITY(1, 1) NOT NULL,
"BRANCH_ID" BIGINT NOT NULL,
"XID" VARCHAR(100) NOT NULL,
"CONTEXT" VARCHAR(128) NOT NULL,
"ROLLBACK_INFO" BLOB NOT NULL,
"LOG_STATUS" INT NOT NULL,
"LOG_CREATED" TIMESTAMP(0) NOT NULL,
"LOG_MODIFIED" TIMESTAMP(0) NOT NULL,
"EXT" VARCHAR(100),
NOT CLUSTER PRIMARY KEY("ID"),
CONSTRAINT "UX_UNDO_LOG" UNIQUE("XID", "BRANCH_ID")) STORAGE(ON "MAIN", CLUSTERBTR) ;

CREATE UNIQUE INDEX "PRIMARY" ON "UNDO_LOG"("ID" ASC) STORAGE(ON "MAIN", CLUSTERBTR) ;