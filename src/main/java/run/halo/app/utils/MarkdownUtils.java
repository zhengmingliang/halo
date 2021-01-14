package run.halo.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.media.tags.MediaTagsExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.io.IoUtil;
import nonapi.io.github.classgraph.json.JSONUtils;
import run.halo.app.model.support.HaloConst;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown utils.
 *
 * @author ryanwang
 * @date 2019-06-27
 */
public class MarkdownUtils {

    private static final DataHolder OPTIONS = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(AttributesExtension.create(),
            AutolinkExtension.create(),
            EmojiExtension.create(),
            EscapedCharacterExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            InsExtension.create(),
            MediaTagsExtension.create(),
            TablesExtension.create(),
            TocExtension.create(),
            SuperscriptExtension.create(),
            YamlFrontMatterExtension.create(),
            GitLabExtension.create()))
            .set(TocExtension.LEVELS, 255)
            .set(TablesExtension.WITH_CAPTION, false)
            .set(TablesExtension.COLUMN_SPANS, false)
            .set(TablesExtension.MIN_SEPARATOR_DASHES, 1)
            .set(TablesExtension.MIN_HEADER_ROWS, 1)
            .set(TablesExtension.MAX_HEADER_ROWS, 1)
            .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
            .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
            .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
            .set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.EMOJI_CHEAT_SHEET)
            .set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY)
            .set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    private static final Parser PARSER = Parser.builder(OPTIONS).build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();
    private static final Pattern FRONT_MATTER = Pattern.compile("^---[\\s\\S]*?---");

    //    /**
    //     * Render html document to markdown document.
    //     *
    //     * @param html html document
    //     * @return markdown document
    //     */
    //    public static String renderMarkdown(String html) {
    //        return FlexmarkHtmlParser.parse(html);
    //    }

    /**
     * Render Markdown content
     *
     * @param markdown content
     * @return String
     */
    public static String renderHtml(String markdown) {
        if (StringUtils.isBlank(markdown)) {
            return StringUtils.EMPTY;
        }

        // Render netease music short url.
        if (markdown.contains(HaloConst.NETEASE_MUSIC_PREFIX)) {
            markdown = markdown.replaceAll(HaloConst.NETEASE_MUSIC_REG_PATTERN, HaloConst.NETEASE_MUSIC_IFRAME);
        }

        // Render bilibili video short url.
        if (markdown.contains(HaloConst.BILIBILI_VIDEO_PREFIX)) {
            markdown = markdown.replaceAll(HaloConst.BILIBILI_VIDEO_REG_PATTERN, HaloConst.BILIBILI_VIDEO_IFRAME);
        }

        // Render youtube video short url.
        if (markdown.contains(HaloConst.YOUTUBE_VIDEO_PREFIX)) {
            markdown = markdown.replaceAll(HaloConst.YOUTUBE_VIDEO_REG_PATTERN, HaloConst.YOUTUBE_VIDEO_IFRAME);
        }

        Node document = PARSER.parse(markdown);

        return RENDERER.render(document);
    }

    /**
     * Get front-matter
     *
     * @param markdown markdown
     * @return Map
     */
    public static Map<String, List<String>> getFrontMatter(String markdown) {
        AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
        Node document = PARSER.parse(markdown);
        visitor.visit(document);
        return visitor.getData();
    }

    public static Map<String, List<String>> getFrontMatterData(String markdown) {
        String[] split = markdown.split("---", 2);
        Map<String, List<String>> data = null;
        if(split.length > 1){
            data = Maps.newHashMap();
            String[] datas = split[0].split("\r\n|\n");
            for (String line : datas) {
                String[] values = line.split(":");
                if(values.length > 1){
                    String value = values[1].trim().replaceAll("\"|'", "");
                    if(value.startsWith("[") && value.endsWith("]")){
                        data.put(values[0].trim(),new Gson().fromJson(value,new ParameterizedType(){
                            @Override
                            public Type[] getActualTypeArguments() {
                                return new Type[]{String.class};
                            }

                            @Override
                            public Type getRawType() {
                                return ArrayList.class;
                            }

                            @Override
                            public Type getOwnerType() {
                                return null;
                            }
                        }));
                    }else{
                        data.put(values[0].trim(),Arrays.asList(value));
                    }
                }
            }
        }
        return data;
    }

    public static void main(String[] args) throws FileNotFoundException, JsonProcessingException {
        File file = new File("C:\\Users\\zml\\Downloads\\Compressed\\solo-hexo-20210113155519\\solo-hexo-20210113155519\\posts\\201612\\Java判断文件编码工具类.md");
        String markdown = IoUtil.read(new FileInputStream(file), StandardCharsets.UTF_8);
        String[] split = markdown.split("---", 2);
        Map<String, String> data = null;
        if(split.length > 1){
            data = Maps.newHashMap();
            String[] datas = split[0].split("\r\n|\n");
            for (String line : datas) {
                String[] values = line.split(":");
                if(values.length > 1){
                    data.put(values[0].trim(),values[1].trim().replaceAll("\"|'",""));
                }
            }
        }
        System.out.println("JsonUtils.objectToJson(data) = " + JsonUtils.objectToJson(data));
    }

    /**
     * remove front matter
     *
     * @param markdown markdown
     * @return markdown
     */
    public static String removeFrontMatter(String markdown) {
        markdown = markdown.trim();
        Matcher matcher = FRONT_MATTER.matcher(markdown);
        if (matcher.find()) {
            return markdown.replace(matcher.group(), "");
        }
        return markdown;
    }

    /**
     * remove front matter
     *
     * @param markdown markdown
     * @return markdown
     */
    public static String removeFrontMatterData(String markdown) {
        String[] split = markdown.split("---", 2);
        Map<String, String> data = null;
        if(split.length > 1){
           return split[1];
        }
        return markdown;
    }
}
