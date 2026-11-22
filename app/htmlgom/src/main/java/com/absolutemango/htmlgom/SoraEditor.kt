package com.absolutemango.htmlgom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import org.eclipse.tm4e.core.registry.IThemeSource

private var isTextMateInitialized = false

@Composable
fun SoraEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!isTextMateInitialized) {
            try {
                FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))
                
                val themeRegistry = ThemeRegistry.getInstance()
                val path = "textmate/darcula.json"
                
                themeRegistry.loadTheme(
                    ThemeModel(
                        IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                        ), "darcula"
                    ).apply { isDark = true }
                )
                themeRegistry.setTheme("darcula")
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                
                isTextMateInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CodeEditor(ctx).apply {
                // Set the theme before setting the language
                ensureTextmateTheme(this)
                
                isLineNumberEnabled = true
                setLineSpacing(2f, 1.1f)
                
                // Professional Features
                props.stickyScroll = true
                isWordwrap = true
                nonPrintablePaintingFlags = CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or 
                                           CodeEditor.FLAG_DRAW_LINE_SEPARATOR or 
                                           CodeEditor.FLAG_DRAW_SOFT_WRAP
                
                getComponent(io.github.rosemoe.sora.widget.component.EditorAutoCompletion::class.java).setEnabledAnimation(true)
                
                // Set language to HTML
                try {
                    val language = TextMateLanguage.create("text.html.basic", true)
                    setEditorLanguage(language)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                setText(text)
                
                subscribeAlways<ContentChangeEvent> {
                    onTextChange(this@apply.text.toString())
                }
            }
        },
        update = { editor ->
            if (editor.text.toString() != text) {
                editor.setText(text)
            }
        }
    )
}

private fun ensureTextmateTheme(editor: CodeEditor) {
    var editorColorScheme = editor.colorScheme
    if (editorColorScheme !is TextMateColorScheme) {
        editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        editor.colorScheme = editorColorScheme
    }
}
