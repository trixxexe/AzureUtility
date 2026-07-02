package com.example.ui.codeeditor

object LanguageDefinitions {

    val keywords = mapOf(
        "kt" to listOf("package", "import", "class", "interface", "object", "fun", "val", "var", "return", "if", "else", "for", "while", "when", "is", "as", "null", "true", "false", "this", "super", "try", "catch", "finally", "throw", "companion", "private", "protected", "public", "internal", "abstract", "open", "override", "init", "constructor"),
        "java" to listOf("package", "import", "class", "interface", "enum", "void", "public", "private", "protected", "static", "final", "abstract", "extends", "implements", "new", "return", "if", "else", "for", "while", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "throws", "this", "super", "null", "true", "false", "int", "double", "float", "boolean", "char", "long", "short", "byte"),
        "py" to listOf("def", "class", "import", "from", "as", "return", "if", "elif", "else", "for", "while", "break", "continue", "in", "is", "not", "and", "or", "lambda", "try", "except", "finally", "raise", "pass", "None", "True", "False", "global", "nonlocal", "yield", "with", "assert"),
        "js" to listOf("let", "const", "var", "function", "class", "import", "export", "default", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "new", "this", "super", "null", "undefined", "true", "false", "async", "await", "typeof", "instanceof"),
        "ts" to listOf("let", "const", "var", "function", "class", "interface", "type", "import", "export", "default", "return", "if", "else", "for", "while", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "new", "this", "super", "null", "undefined", "true", "false", "async", "await", "typeof", "instanceof", "any", "unknown", "never", "void", "string", "number", "boolean", "readonly", "private", "protected", "public"),
        "c" to listOf("auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register", "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while"),
        "cpp" to listOf("class", "namespace", "using", "template", "typename", "public", "private", "protected", "virtual", "override", "new", "delete", "this", "friend", "inline", "explicit", "operator", "try", "catch", "throw", "const", "static", "void", "int", "double", "float", "bool", "char", "true", "false", "return", "if", "else", "for", "while", "switch", "case", "break", "continue"),
        "rs" to listOf("fn", "let", "mut", "struct", "enum", "impl", "trait", "pub", "use", "mod", "as", "match", "if", "else", "for", "while", "loop", "break", "continue", "return", "unsafe", "self", "Self", "true", "false", "nil", "static", "const", "type", "where"),
        "go" to listOf("package", "import", "func", "struct", "interface", "var", "const", "type", "return", "if", "else", "for", "range", "switch", "case", "default", "break", "continue", "fallthrough", "go", "select", "chan", "map", "nil", "true", "false"),
        "rb" to listOf("def", "end", "class", "module", "require", "return", "if", "elsif", "else", "unless", "for", "while", "until", "break", "next", "nil", "true", "false", "and", "or", "not", "self", "super", "begin", "rescue", "ensure"),
        "php" to listOf("function", "class", "interface", "trait", "namespace", "use", "return", "if", "elseif", "else", "for", "foreach", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "new", "this", "null", "true", "false", "echo", "print", "public", "private", "protected", "static", "const"),
        "sh" to listOf("if", "then", "elif", "else", "fi", "for", "in", "while", "until", "do", "done", "case", "esac", "function", "return", "exit", "local", "alias", "export"),
        "sql" to listOf("select", "from", "where", "insert", "into", "values", "update", "set", "delete", "create", "table", "drop", "index", "alter", "join", "inner", "left", "right", "outer", "on", "group", "by", "order", "having", "and", "or", "not", "null", "true", "false", "primary", "key", "foreign", "references", "index"),
        "html" to listOf("DOCTYPE", "html", "head", "title", "body", "div", "span", "p", "a", "h1", "h2", "h3", "h4", "h5", "h6", "img", "br", "hr", "table", "tr", "td", "th", "thead", "tbody", "form", "input", "button", "select", "option", "textarea", "label", "link", "script", "style", "meta"),
        "css" to listOf("margin", "padding", "border", "background", "color", "font", "display", "position", "top", "bottom", "left", "right", "width", "height", "box-shadow", "border-radius", "flex", "grid", "align-items", "justify-content", "transition", "animation", "transform"),
        "json" to listOf("true", "false", "null"),
        "xml" to listOf("version", "encoding", "standalone"),
        "toml" to listOf("true", "false"),
        "yaml" to listOf("true", "false", "null")
    )
}
