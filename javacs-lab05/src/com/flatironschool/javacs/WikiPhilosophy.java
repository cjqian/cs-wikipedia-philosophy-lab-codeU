/* Wikipedia Philosophy Lab
 * Attempts to find a path to the Philsophy wikipedia page from the first lowercase link on any page.
 * Throws an error in the case of no link found on a page, or a repeated link on the chain.
 *
 * Author: Crystal Qian
 */

package com.flatironschool.javacs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.select.Elements;

public class WikiPhilosophy {
    final static WikiFetcher wf = new WikiFetcher();
    final static String baseURL = "https://en.wikipedia.org";
    final static String targetURL = baseURL + "/wiki/Philosophy";
    static List<String> visitedURLs = new ArrayList<String>();

    //checks if a url is already in our visitedURLs list, and returns 
    //true or false depending
    private static boolean hasSeen(String url){
        Iterator iterator = visitedURLs.iterator();

        //check all URLs, returning True if have seen
        while (iterator.hasNext()){
            if (iterator.next().equals(url)){
                return true;
            }
        }

        //else, we haven't seen
        return false;
    }

    //returns true if the current url
    // *is not in italics/parentheses
    // *is not empty
    // *text doesn't start with uppercase letter
    // *points to an external link
    // and false otherwise
    private static boolean isValid(Element element){
        String href = element.attr("abs:href");
        String text = element.ownText();

        //check up to the parent to make sure this tag is not in italics
        Element temp = element;
        while (temp != null){
            if (temp.tagName().equals("i") 
                    || temp.tagName().equals("em") 
                    || temp.tagName().equals("var")) 
                return false;

            temp = temp.parent();
        }

        //in parentheses
        if (text.startsWith("(")
                && text.endsWith(")"))
            return false;

        //make sure the link is not empty
        if (href.equals("")) return false;

        //makes sure the link is not red
        if (text.startsWith("[[") 
                && text.endsWith("]]")) 
            return false;

        //should not point to an external link
        if (!href.startsWith(baseURL)) return false;

        //makes sure the link doesn't start with uppercase letter
        if (Character.isUpperCase(text.charAt(0))) return false;

        return true; 
    }


    //given an iterator over all elements of a paragraph and the current url,
    //returns the next url that
    //if there are no such links, we return null
    //if the link has already been seen, we throw an exception
    private static String getUrlFromParagraph(Iterable<Node> iterator, String url){
        //traverse each node in the paragraph
        for (Node node : iterator){
            //if has attribute "href"
            if (node instanceof Element && node.hasAttr("href")){
                Element element = (Element)node;

                //we want the absolute link
                if (isValid(element)){
                    String linkurl = node.attr("abs:href");
                    //if the link is one we have already seen, throw an error 
                    if (hasSeen(linkurl)){
                        //indicate the error and exit
                        System.out.println("ERROR: This link has already been seen.");
                        System.exit(0);
                    }

                    return node.attr("abs:href");
                }
            }
        }

        //we didn't find any good links in this paragraph
        return null;
    }

    //populates our visitedURLs recursively until termination
    //exists if the page has no links
    private static void update(String url) throws IOException{
        String curURL = url;
        //we add the current URL to our list, since we're checking it
        visitedURLs.add(curURL);

        //check if this is the Wikipedia page
        if (curURL.equals(targetURL)){
            System.out.println("SUCCESS: Target URL found!");
            return;
        }

        //takes a URL for a Wikipedia page and downloads it
        Elements paragraphs = wf.fetchWikipedia(url);

        int i = 0;
        int n = paragraphs.size();
        boolean doneTraversing = false;

        //we scan all paragraphs until we get to the first link
        while (!doneTraversing && i < n){
            Element paragraph = paragraphs.get(i);
            Iterable<Node> iterator = new WikiNodeIterable(paragraph);

            //gets the first valid link that we haven't seen
            curURL = getUrlFromParagraph(iterator, curURL);

            //if a valid URL was found, 
            //we recursively check the next link
            if (curURL != null){
                doneTraversing = true;
                update(curURL);
            }     

            //else, we just look at the next paragraph
            i++;
        }

        //if we looked at all paragraphs and there were no links, we return a failure
        if (!doneTraversing){
            System.out.println("ERROR: There were no valid links in the page.");
            System.exit(0);
        }
    }

    //prints all items in the list
    private static void printList(){
        Iterator iterator = visitedURLs.iterator();

        while (iterator.hasNext()){
            System.out.println(iterator.next());
        }
    }

    /**
     * Tests a conjecture about Wikipedia and Philosophy.
     * 
     * https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy
     * 
     * 1. Clicking on the first non-parenthesized, non-italicized link
     * 2. Ignoring external links, links to the current page, or red links
     * 3. Stopping when reaching "Philosophy", a page with no links or a page
     *    that does not exist, or when a loop occurs
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        //takes in and updates intial string
        String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
        update(url);
        printList();
    }
}
