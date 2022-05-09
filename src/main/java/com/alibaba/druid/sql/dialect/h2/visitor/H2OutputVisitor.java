/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.h2.visitor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;

import java.util.List;
import java.util.Map;

public class H2OutputVisitor extends SQLASTOutputVisitor implements H2ASTVisitor {
    public H2OutputVisitor(Appendable appender) {
        super(appender, DbType.h2);
    }

    public H2OutputVisitor(Appendable appender, DbType dbType) {
        super(appender, dbType);
    }

    public H2OutputVisitor(Appendable appender, boolean parameterized) {
        super(appender, parameterized);
        dbType = DbType.h2;
    }

    public boolean visit(SQLReplaceStatement x) {
        print0(ucase ? "MERGE INTO " : "merge into ");

        printTableSourceExpr(x.getTableName());

        List<SQLExpr> columns = x.getColumns();
        if (columns.size() > 0) {
            print0(ucase ? " KEY (" : " key (");
            for (int i = 0, size = columns.size(); i < size; ++i) {
                if (i != 0) {
                    print0(", ");
                }

                SQLExpr columnn = columns.get(i);
                printExpr(columnn, parameterized);
            }
            print(')');
        }

        List<SQLInsertStatement.ValuesClause> valuesClauseList = x.getValuesList();
        if (!valuesClauseList.isEmpty()) {
            println();
            print0(ucase ? "VALUES " : "values ");
            int size = valuesClauseList.size();
            if (size == 0) {
                print0("()");
            } else {
                for (int i = 0; i < size; ++i) {
                    if (i != 0) {
                        print0(", ");
                    }
                    visit(valuesClauseList.get(i));
                }
            }
        }

        SQLQueryExpr query = x.getQuery();
        if (query != null) {
            visit(query);
        }

        return false;
    }

    @Override
    public boolean visit(SQLCreateDatabaseStatement x) {

        /*
        https://h2database.com/html/commands.html#create_schema
        CREATE SCHEMA [ IF NOT EXISTS ]
        { name [ AUTHORIZATION ownerName ] | [ AUTHORIZATION ownerName ] }
        [ WITH tableEngineParamName [,...] ]
         */

        printUcase("CREATE SCHEMA ");

        if (x.isIfNotExists()) {
            printUcase("IF NOT EXISTS ");
        }
        x.getName().accept(this);

        return false;
    }

    @Override
    public boolean visit(SQLCreateIndexStatement x) {

        /* h2 CREATE INDEX BNF: https://h2database.com/html/commands.html#create_index
        CREATE [ UNIQUE | SPATIAL ] INDEX
        [ [ IF NOT EXISTS ] [schemaName.]indexName ]
        ON [schemaName.]tableName ( indexColumn [,...] )
        [ INCLUDE ( indexColumn [,...] ) ]
         */

        printUcase("CREATE ");

        String type = x.getType();
        if ( "UNIQUE".equalsIgnoreCase(type) || "SPATIAL".equalsIgnoreCase(type) ) {
            printUcase(type + ' ');
        }

        printUcase("INDEX ");

        if (x.isIfNotExists()) {
            printUcase("IF NOT EXISTS ");
        }

        x.getName().accept(this);

        printUcase(" ON ");
        x.getTable().accept(this);
        print0(" (");
        printAndAccept(x.getItems(), ", ");
        print(')');

        return false;
    }

}
