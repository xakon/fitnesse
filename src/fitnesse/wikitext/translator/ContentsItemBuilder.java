package fitnesse.wikitext.translator;

import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlUtil;
import fitnesse.responders.WikiImportProperty;
import fitnesse.wiki.*;
import fitnesse.wikitext.parser.SourcePage;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.widgets.TOCWidget;
import util.GracefulNamer;

public class ContentsItemBuilder {
    private Symbol contents;
    private int level;

    public ContentsItemBuilder(Symbol contents, int level) {
        this.contents = contents;
        this.level = level;
    }

    public HtmlTag buildLevel(SourcePage page, HtmlTag contentsDiv) {
        HtmlTag div = HtmlUtil.makeDivTag("toc" + level);
        HtmlTag list = new HtmlTag("ul");
        try {
            for (SourcePage child: page.getChildren()) {
                HtmlTag listItem = new HtmlTag("li");
                listItem.add(buildItem(child));
                if (hasOption("-R", "") && child.getChildren().size() > 0) {
                    HtmlTag nestedDiv =  HtmlUtil.makeDivTag("nested-contents");
                    listItem.add(new ContentsItemBuilder(contents, level + 1).buildLevel(child, nestedDiv));
                }
                list.add(listItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        contentsDiv.add(list);
        div.add(contentsDiv);
        return div;
    }

    public HtmlTag buildItem(SourcePage page) {
        HtmlTag result = new HtmlTag("a", buildBody(page));
        result.addAttribute("href", buildReference(page));
        String help = page.getProperty(PageData.PropertyHELP);
        if (help.length() > 0) {
            if (hasOption("-h", TOCWidget.HELP_TOC)) {
                result.tail = HtmlUtil.makeSpanTag("pageHelp", ": " + help).htmlInline();
            }
            else {
                result.addAttribute("title", help);
            }
        }
        return result;
    }

    private String buildBody(SourcePage page) {
        String itemText = page.getName();

        if (hasOption("-g", TOCWidget.REGRACE_TOC)) {
            //todo: DRY? see wikiwordbuilder
            itemText = GracefulNamer.regrace(itemText);
        }

        if (hasOption("-p", TOCWidget.PROPERTY_TOC)) {
            String properties = getBooleanProperties(page);
            if (properties.length() > 0) itemText += " " + properties;
        }

        if (hasOption("-f", TOCWidget.FILTER_TOC)) {
            String filters = page.getProperty(PageData.PropertySUITES);
            if (filters.length() > 0) itemText += " (" + filters + ")";
        }
        
        return itemText;
    }

    private String buildReference(SourcePage sourcePage) {
        return sourcePage.getFullName();
    }

    private boolean hasOption(String option, String variableName) {
        for (Symbol child: contents.getChildren()) {
           if (child.getContent().equals(option)) return true;
        }
        return variableName.length() > 0
                && contents.getVariable(variableName, "").equals("true");
    }

    private String getBooleanProperties(SourcePage sourcePage) {
        String result = "";
        if (sourcePage.hasProperty(PageType.SUITE.toString())) result += "*";
        if (sourcePage.hasProperty(PageType.TEST.toString())) result += "+";
        if (sourcePage.hasProperty(WikiImportProperty.PROPERTY_NAME)) result += "@";
        if (sourcePage.hasProperty(PageData.PropertyPRUNE)) result += "-";
        return result;
    }
}
