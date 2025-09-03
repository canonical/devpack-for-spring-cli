grammar GradlePlugin;

@header {
package com.canonical.devpackspring.build.grammar;
}

sequence : anything (plugin anything)? EOF;

plugin: PLUGINS LBRACE plugin_block RBRACE;

plugin_block: (~RBRACE)*;

anything: (~(PLUGINS | EOF))*;

PLUGINS     : 'plugins';
LBRACE     : '{';
RBRACE     : '}';
WHITESPACE : [ \t\r\n]+ -> skip;
ANY        : . ;