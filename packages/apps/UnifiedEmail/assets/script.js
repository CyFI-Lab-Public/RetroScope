/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

var BLOCKED_SRC_ATTR = "blocked-src";

// the set of Elements currently scheduled for processing in handleAllImageLoads
// this is an Array, but we treat it like a Set and only insert unique items
var gImageLoadElements = [];

var gScaleInfo;

/**
 * Only revert transforms that do an imperfect job of shrinking content if they fail
 * to shrink by this much. Expressed as a ratio of:
 * (original width difference : width difference after transforms);
 */
var TRANSFORM_MINIMUM_EFFECTIVE_RATIO = 0.7;

// Don't ship with this on.
var DEBUG_DISPLAY_TRANSFORMS = false;

var gTransformText = {};

/**
 * Returns the page offset of an element.
 *
 * @param {Element} element The element to return the page offset for.
 * @return {left: number, top: number} A tuple including a left and top value representing
 *     the page offset of the element.
 */
function getTotalOffset(el) {
    var result = {
        left: 0,
        top: 0
    };
    var parent = el;

    while (parent) {
        result.left += parent.offsetLeft;
        result.top += parent.offsetTop;
        parent = parent.offsetParent;
    }

    return result;
}

/**
 * Walks up the DOM starting at a given element, and returns an element that has the
 * specified class name or null.
 */
function up(el, className) {
    var parent = el;
    while (parent) {
        if (parent.classList && parent.classList.contains(className)) {
            break;
        }
        parent = parent.parentNode;
    }
    return parent || null;
}

function getCachedValue(div, property, attrName) {
    var value;
    if (div.hasAttribute(attrName)) {
        value = div.getAttribute(attrName);
    } else {
        value = div[property];
        div.setAttribute(attrName, value);
    }
    return value;
}

function onToggleClick(e) {
    toggleQuotedText(e.target);
    measurePositions();
}

function toggleQuotedText(toggleElement) {
    var elidedTextElement = toggleElement.nextSibling;
    var isHidden = getComputedStyle(elidedTextElement).display == 'none';
    toggleElement.innerHTML = isHidden ? MSG_HIDE_ELIDED : MSG_SHOW_ELIDED;
    elidedTextElement.style.display = isHidden ? 'block' : 'none';

    // Revealing the elided text should normalize it to fit-width to prevent
    // this message from blowing out the conversation width.
    if (isHidden) {
        normalizeElementWidths([elidedTextElement]);
    }
}

function collapseAllQuotedText() {
    processQuotedText(document.documentElement, false /* showElided */);
}

function processQuotedText(elt, showElided) {
    var i;
    var elements = elt.getElementsByClassName("elided-text");
    var elidedElement, toggleElement;
    for (i = 0; i < elements.length; i++) {
        elidedElement = elements[i];
        toggleElement = document.createElement("div");
        toggleElement.className = "mail-elided-text";
        toggleElement.innerHTML = MSG_SHOW_ELIDED;
        toggleElement.onclick = onToggleClick;
        elidedElement.style.display = 'none';
        elidedElement.parentNode.insertBefore(toggleElement, elidedElement);
        if (showElided) {
            toggleQuotedText(toggleElement);
        }
    }
}

function isConversationEmpty(bodyDivs) {
    var i, len;
    var msgBody;
    var text;

    // Check if given divs are empty (in appearance), and disable zoom if so.
    for (i = 0, len = bodyDivs.length; i < len; i++) {
        msgBody = bodyDivs[i];
        // use 'textContent' to exclude markup when determining whether bodies are empty
        // (fall back to more expensive 'innerText' if 'textContent' isn't implemented)
        text = msgBody.textContent || msgBody.innerText;
        if (text.trim().length > 0) {
            return false;
        }
    }
    return true;
}

function normalizeAllMessageWidths() {
    var expandedBodyDivs;
    var metaViewport;
    var contentValues;
    var isEmpty;

    expandedBodyDivs = document.querySelectorAll(".expanded > .mail-message-content");

    isEmpty = isConversationEmpty(expandedBodyDivs);

    normalizeElementWidths(expandedBodyDivs);

    // assemble a working <meta> viewport "content" value from the base value in the
    // document, plus any dynamically determined options
    metaViewport = document.getElementById("meta-viewport");
    contentValues = [metaViewport.getAttribute("content")];
    if (isEmpty) {
        contentValues.push(metaViewport.getAttribute("data-zoom-off"));
    } else {
        contentValues.push(metaViewport.getAttribute("data-zoom-on"));
    }
    metaViewport.setAttribute("content", contentValues.join(","));
}

/*
 * Normalizes the width of all elements supplied to the document body's overall width.
 * Narrower elements are zoomed in, and wider elements are zoomed out.
 * This method is idempotent.
 */
function normalizeElementWidths(elements) {
    var i;
    var el;
    var documentWidth;
    var newZoom, oldZoom;

    documentWidth = document.body.offsetWidth;

    for (i = 0; i < elements.length; i++) {
        el = elements[i];
        oldZoom = el.style.zoom;
        // reset any existing normalization
        if (oldZoom) {
            el.style.zoom = 1;
        }
        newZoom = documentWidth / el.scrollWidth;
        transformContent(el, documentWidth, el.scrollWidth);
        newZoom = documentWidth / el.scrollWidth;
        if (NORMALIZE_MESSAGE_WIDTHS) {
            el.style.zoom = newZoom;
        }
    }
}

function transformContent(el, docWidth, elWidth) {
    var nodes;
    var i, len;
    var index;
    var newWidth = elWidth;
    var wStr;
    var touched;
    // the format of entries in this array is:
    // entry := [ undoFunction, undoFunctionThis, undoFunctionParamArray ]
    var actionLog = [];
    var node;
    var done = false;
    var msgId;
    var transformText;
    var existingText;
    var textElement;
    var start;
    var beforeWidth;
    var tmpActionLog = [];
    if (elWidth <= docWidth) {
        return;
    }

    start = Date.now();

    if (el.parentElement.classList.contains("mail-message")) {
        msgId = el.parentElement.id;
        transformText = "[origW=" + elWidth + "/" + docWidth;
    }

    // Try munging all divs or textareas with inline styles where the width
    // is wider than docWidth, and change it to be a max-width.
    touched = false;
    nodes = ENABLE_MUNGE_TABLES ? el.querySelectorAll("div[style], textarea[style]") : [];
    touched = transformBlockElements(nodes, docWidth, actionLog);
    if (touched) {
        newWidth = el.scrollWidth;
        console.log("ran div-width munger on el=" + el + " oldW=" + elWidth + " newW=" + newWidth
            + " docW=" + docWidth);
        if (msgId) {
            transformText += " DIV:newW=" + newWidth;
        }
        if (newWidth <= docWidth) {
            done = true;
        }
    }

    if (!done) {
        // OK, that wasn't enough. Find images with widths and override their widths.
        nodes = ENABLE_MUNGE_IMAGES ? el.querySelectorAll("img") : [];
        touched = transformImages(nodes, docWidth, actionLog);
        if (touched) {
            newWidth = el.scrollWidth;
            console.log("ran img munger on el=" + el + " oldW=" + elWidth + " newW=" + newWidth
                + " docW=" + docWidth);
            if (msgId) {
                transformText += " IMG:newW=" + newWidth;
            }
            if (newWidth <= docWidth) {
                done = true;
            }
        }
    }

    if (!done) {
        // OK, that wasn't enough. Find tables with widths and override their widths.
        // Also ensure that any use of 'table-layout: fixed' is negated, since using
        // that with 'width: auto' causes erratic table width.
        nodes = ENABLE_MUNGE_TABLES ? el.querySelectorAll("table") : [];
        touched = addClassToElements(nodes, shouldMungeTable, "munged",
            actionLog);
        if (touched) {
            newWidth = el.scrollWidth;
            console.log("ran table munger on el=" + el + " oldW=" + elWidth + " newW=" + newWidth
                + " docW=" + docWidth);
            if (msgId) {
                transformText += " TABLE:newW=" + newWidth;
            }
            if (newWidth <= docWidth) {
                done = true;
            }
        }
    }

    if (!done) {
        // OK, that wasn't enough. Try munging all <td> to override any width and nowrap set.
        beforeWidth = newWidth;
        nodes = ENABLE_MUNGE_TABLES ? el.querySelectorAll("td") : [];
        touched = addClassToElements(nodes, null /* mungeAll */, "munged",
            tmpActionLog);
        if (touched) {
            newWidth = el.scrollWidth;
            console.log("ran td munger on el=" + el + " oldW=" + elWidth + " newW=" + newWidth
                + " docW=" + docWidth);
            if (msgId) {
                transformText += " TD:newW=" + newWidth;
            }
            if (newWidth <= docWidth) {
                done = true;
            } else if (newWidth == beforeWidth) {
                // this transform did not improve things, and it is somewhat risky.
                // back it out, since it's the last transform and we gained nothing.
                undoActions(tmpActionLog);
            } else {
                // the transform WAS effective (although not 100%)
                // copy the temporary action log entries over as normal
                for (i = 0, len = tmpActionLog.length; i < len; i++) {
                    actionLog.push(tmpActionLog[i]);
                }
            }
        }
    }

    // If the transformations shrank the width significantly enough, leave them in place.
    // We figure that in those cases, the benefits outweight the risk of rendering artifacts.
    if (!done && (elWidth - newWidth) / (elWidth - docWidth) >
            TRANSFORM_MINIMUM_EFFECTIVE_RATIO) {
        console.log("transform(s) deemed effective enough");
        done = true;
    }

    if (done) {
        if (msgId) {
            transformText += "]";
            existingText = gTransformText[msgId];
            if (!existingText) {
                transformText = "Message transforms: " + transformText;
            } else {
                transformText = existingText + " " + transformText;
            }
            gTransformText[msgId] = transformText;
            window.mail.onMessageTransform(msgId, transformText);
            if (DEBUG_DISPLAY_TRANSFORMS) {
                textElement = el.firstChild;
                if (!textElement.classList || !textElement.classList.contains("transform-text")) {
                    textElement = document.createElement("div");
                    textElement.classList.add("transform-text");
                    textElement.style.fontSize = "10px";
                    textElement.style.color = "#ccc";
                    el.insertBefore(textElement, el.firstChild);
                }
                textElement.innerHTML = transformText + "<br>";
            }
        }
        console.log("munger(s) succeeded, elapsed time=" + (Date.now() - start));
        return;
    }

    // reverse all changes if the width is STILL not narrow enough
    // (except the width->maxWidth change, which is not particularly destructive)
    undoActions(actionLog);
    if (actionLog.length > 0) {
        console.log("all mungers failed, changes reversed. elapsed time=" + (Date.now() - start));
    }
}

function undoActions(actionLog) {
    for (i = 0, len = actionLog.length; i < len; i++) {
        actionLog[i][0].apply(actionLog[i][1], actionLog[i][2]);
    }
}

function addClassToElements(nodes, conditionFn, classToAdd, actionLog) {
    var i, len;
    var node;
    var added = false;
    for (i = 0, len = nodes.length; i < len; i++) {
        node = nodes[i];
        if (!conditionFn || conditionFn(node)) {
            if (node.classList.contains(classToAdd)) {
                continue;
            }
            node.classList.add(classToAdd);
            added = true;
            actionLog.push([node.classList.remove, node.classList, [classToAdd]]);
        }
    }
    return added;
}

function transformBlockElements(nodes, docWidth, actionLog) {
    var i, len;
    var node;
    var wStr;
    var index;
    var touched = false;

    for (i = 0, len = nodes.length; i < len; i++) {
        node = nodes[i];
        wStr = node.style.width || node.style.minWidth;
        index = wStr ? wStr.indexOf("px") : -1;
        if (index >= 0 && wStr.slice(0, index) > docWidth) {
            saveStyleProperty(node, "width", actionLog);
            saveStyleProperty(node, "minWidth", actionLog);
            saveStyleProperty(node, "maxWidth", actionLog);
            node.style.width = "100%";
            node.style.minWidth = "";
            node.style.maxWidth = wStr;
            touched = true;
        }
    }
    return touched;
}

function transformImages(nodes, docWidth, actionLog) {
    var i, len;
    var node;
    var w, h;
    var touched = false;

    for (i = 0, len = nodes.length; i < len; i++) {
        node = nodes[i];
        w = node.offsetWidth;
        h = node.offsetHeight;
        // shrink w/h proportionally if the img is wider than available width
        if (w > docWidth) {
            saveStyleProperty(node, "maxWidth", actionLog);
            saveStyleProperty(node, "width", actionLog);
            saveStyleProperty(node, "height", actionLog);
            node.style.maxWidth = docWidth + "px";
            node.style.width = "100%";
            node.style.height = "auto";
            touched = true;
        }
    }
    return touched;
}

function saveStyleProperty(node, property, actionLog) {
    var savedName = "data-" + property;
    node.setAttribute(savedName, node.style[property]);
    actionLog.push([undoSetProperty, node, [property, savedName]]);
}

function undoSetProperty(property, savedProperty) {
    this.style[property] = savedProperty ? this.getAttribute(savedProperty) : "";
}

function shouldMungeTable(table) {
    return table.hasAttribute("width") || table.style.width;
}

function hideAllUnsafeImages() {
    hideUnsafeImages(document.getElementsByClassName("mail-message-content"));
}

function hideUnsafeImages(msgContentDivs) {
    var i, msgContentCount;
    var j, imgCount;
    var msgContentDiv, image;
    var images;
    var showImages;
    for (i = 0, msgContentCount = msgContentDivs.length; i < msgContentCount; i++) {
        msgContentDiv = msgContentDivs[i];
        showImages = msgContentDiv.classList.contains("mail-show-images");

        images = msgContentDiv.getElementsByTagName("img");
        for (j = 0, imgCount = images.length; j < imgCount; j++) {
            image = images[j];
            rewriteRelativeImageSrc(image);
            attachImageLoadListener(image);
            // TODO: handle inline image attachments for all supported protocols
            if (!showImages) {
                blockImage(image);
            }
        }
    }
}

/**
 * Changes relative paths to absolute path by pre-pending the account uri
 * @param {Element} imgElement Image for which the src path will be updated.
 */
function rewriteRelativeImageSrc(imgElement) {
    var src = imgElement.src;

    // DOC_BASE_URI will always be a unique x-thread:// uri for this particular conversation
    if (src.indexOf(DOC_BASE_URI) == 0 && (DOC_BASE_URI != CONVERSATION_BASE_URI)) {
        // The conversation specifies a different base uri than the document
        src = CONVERSATION_BASE_URI + src.substring(DOC_BASE_URI.length);
        imgElement.src = src;
    }
};


function attachImageLoadListener(imageElement) {
    // Reset the src attribute to the empty string because onload will only fire if the src
    // attribute is set after the onload listener.
    var originalSrc = imageElement.src;
    imageElement.src = '';
    imageElement.onload = imageOnLoad;
    imageElement.src = originalSrc;
}

/**
 * Handle an onload event for an <img> tag.
 * The image could be within an elided-text block, or at the top level of a message.
 * When a new image loads, its new bounds may affect message or elided-text geometry,
 * so we need to inspect and adjust the enclosing element's zoom level where necessary.
 *
 * Because this method can be called really often, and zoom-level adjustment is slow,
 * we collect the elements to be processed and do them all later in a single deferred pass.
 */
function imageOnLoad(e) {
    // normalize the quoted text parent if we're in a quoted text block, or else
    // normalize the parent message content element
    var parent = up(e.target, "elided-text") || up(e.target, "mail-message-content");
    if (!parent) {
        // sanity check. shouldn't really happen.
        return;
    }

    // if there was no previous work, schedule a new deferred job
    if (gImageLoadElements.length == 0) {
        window.setTimeout(handleAllImageOnLoads, 0);
    }

    // enqueue the work if it wasn't already enqueued
    if (gImageLoadElements.indexOf(parent) == -1) {
        gImageLoadElements.push(parent);
    }
}

// handle all deferred work from image onload events
function handleAllImageOnLoads() {
    normalizeElementWidths(gImageLoadElements);
    measurePositions();
    // clear the queue so the next onload event starts a new job
    gImageLoadElements = [];
}

function blockImage(imageElement) {
    var src = imageElement.src;
    if (src.indexOf("http://") == 0 || src.indexOf("https://") == 0 ||
            src.indexOf("content://") == 0) {
        imageElement.setAttribute(BLOCKED_SRC_ATTR, src);
        imageElement.src = "data:";
    }
}

function setWideViewport() {
    var metaViewport = document.getElementById('meta-viewport');
    metaViewport.setAttribute('content', 'width=' + WIDE_VIEWPORT_WIDTH);
}

function restoreScrollPosition() {
    var scrollYPercent = window.mail.getScrollYPercent();
    if (scrollYPercent && document.body.offsetHeight > window.innerHeight) {
        document.body.scrollTop = Math.floor(scrollYPercent * document.body.offsetHeight);
    }
}

function onContentReady(event) {
    window.mail.onContentReady();
}

function setupContentReady() {
    var signalDiv;

    // PAGE READINESS SIGNAL FOR JELLYBEAN AND NEWER
    // Notify the app on 'webkitAnimationStart' of a simple dummy element with a simple no-op
    // animation that immediately runs on page load. The app uses this as a signal that the
    // content is loaded and ready to draw, since WebView delays firing this event until the
    // layers are composited and everything is ready to draw.
    //
    // This code is conditionally enabled on JB+ by setting the ENABLE_CONTENT_READY flag.
    if (ENABLE_CONTENT_READY) {
        signalDiv = document.getElementById("initial-load-signal");
        signalDiv.addEventListener("webkitAnimationStart", onContentReady, false);
    }
}

// BEGIN Java->JavaScript handlers
function measurePositions() {
    var overlayTops, overlayBottoms;
    var i;
    var len;

    var expandedBody, headerSpacer;
    var prevBodyBottom = 0;
    var expandedBodyDivs = document.querySelectorAll(".expanded > .mail-message-content");

    // N.B. offsetTop and offsetHeight of an element with the "zoom:" style applied cannot be
    // trusted.

    overlayTops = new Array(expandedBodyDivs.length + 1);
    overlayBottoms = new Array(expandedBodyDivs.length + 1);
    for (i = 0, len = expandedBodyDivs.length; i < len; i++) {
        expandedBody = expandedBodyDivs[i];
        headerSpacer = expandedBody.previousElementSibling;
        // addJavascriptInterface handler only supports string arrays
        overlayTops[i] = "" + prevBodyBottom;
        overlayBottoms[i] = "" + (getTotalOffset(headerSpacer).top + headerSpacer.offsetHeight);
        prevBodyBottom = getTotalOffset(expandedBody.nextElementSibling).top;
    }
    // add an extra one to mark the top/bottom of the last message footer spacer
    overlayTops[i] = "" + prevBodyBottom;
    overlayBottoms[i] = "" + document.documentElement.scrollHeight;

    window.mail.onWebContentGeometryChange(overlayTops, overlayBottoms);
}

function unblockImages(messageDomIds) {
    var i, j, images, imgCount, image, blockedSrc;
    for (j = 0, len = messageDomIds.length; j < len; j++) {
        var messageDomId = messageDomIds[j];
        var msg = document.getElementById(messageDomId);
        if (!msg) {
            console.log("can't unblock, no matching message for id: " + messageDomId);
            continue;
        }
        images = msg.getElementsByTagName("img");
        for (i = 0, imgCount = images.length; i < imgCount; i++) {
            image = images[i];
            blockedSrc = image.getAttribute(BLOCKED_SRC_ATTR);
            if (blockedSrc) {
                image.src = blockedSrc;
                image.removeAttribute(BLOCKED_SRC_ATTR);
            }
        }
    }
}

function setConversationHeaderSpacerHeight(spacerHeight) {
    var spacer = document.getElementById("conversation-header");
    if (!spacer) {
        console.log("can't set spacer for conversation header");
        return;
    }
    spacer.style.height = spacerHeight + "px";
    measurePositions();
}

function setMessageHeaderSpacerHeight(messageDomId, spacerHeight) {
    var spacer = document.querySelector("#" + messageDomId + " > .mail-message-header");
    if (!spacer) {
        console.log("can't set spacer for message with id: " + messageDomId);
        return;
    }
    spacer.style.height = spacerHeight + "px";
    measurePositions();
}

function setMessageBodyVisible(messageDomId, isVisible, spacerHeight,
        topBorderHeight, bottomBorderHeight) {
    var i, len;
    var visibility = isVisible ? "block" : "none";
    var messageDiv = document.querySelector("#" + messageDomId);
    var collapsibleDivs = document.querySelectorAll("#" + messageDomId + " > .collapsible");
    if (!messageDiv || collapsibleDivs.length == 0) {
        console.log("can't set body visibility for message with id: " + messageDomId);
        return;
    }

    // if the top border has changed, update the height of its spacer
    if (topBorderHeight > 0) {
        var border = messageDiv.previousElementSibling;
        if (!border) {
            console.log("can't set spacer for top border");
            return;
        }
        border.style.height = topBorderHeight + "px";
    }

    // if the bottom border has changed, update the height of its spacer
    if (bottomBorderHeight > 0) {
        var border = messageDiv.nextElementSibling;
        if (!border) {
            console.log("can't set spacer for bottom border");
            return;
        }
        border.style.height = bottomBorderHeight + "px";
    }

    messageDiv.classList.toggle("expanded");
    for (i = 0, len = collapsibleDivs.length; i < len; i++) {
        collapsibleDivs[i].style.display = visibility;
    }

    // revealing new content should trigger width normalization, since the initial render
    // skips collapsed and super-collapsed messages
    if (isVisible) {
        normalizeElementWidths(messageDiv.getElementsByClassName("mail-message-content"));
    }

    setMessageHeaderSpacerHeight(messageDomId, spacerHeight);
}

function replaceSuperCollapsedBlock(startIndex) {
    var parent, block, msg;

    block = document.querySelector(".mail-super-collapsed-block[index='" + startIndex + "']");
    if (!block) {
        console.log("can't expand super collapsed block at index: " + startIndex);
        return;
    }
    parent = block.parentNode;
    block.innerHTML = window.mail.getTempMessageBodies();

    // process the new block contents in one go before we pluck them out of the common ancestor
    processQuotedText(block, false /* showElided */);
    hideUnsafeImages(block.getElementsByClassName("mail-message-content"));

    msg = block.firstChild;
    while (msg) {
        parent.insertBefore(msg, block);
        msg = block.firstChild;
    }
    parent.removeChild(block);
    measurePositions();
}

function replaceMessageBodies(messageIds) {
    var i;
    var id;
    var msgContentDiv;

    for (i = 0, len = messageIds.length; i < len; i++) {
        id = messageIds[i];
        msgContentDiv = document.querySelector("#" + id + " > .mail-message-content");
        msgContentDiv.innerHTML = window.mail.getMessageBody(id);
        processQuotedText(msgContentDiv, true /* showElided */);
        hideUnsafeImages([msgContentDiv]);
    }
    measurePositions();
}

// handle the special case of adding a single new message at the end of a conversation
function appendMessageHtml() {
    var msg = document.createElement("div");
    msg.innerHTML = window.mail.getTempMessageBodies();
    var body = msg.children[0];  // toss the outer div, it was just to render innerHTML into
    var border = msg.children[1]; // get the border spacer as well
    document.body.appendChild(body);
    document.body.appendChild(border);
    processQuotedText(msg, true /* showElided */);
    hideUnsafeImages(msg.getElementsByClassName("mail-message-content"));
    measurePositions();
}

function onScaleBegin(screenX, screenY) {
//    console.log("JS got scaleBegin x/y=" + screenX + "/" + screenY);
    var focusX = screenX + document.body.scrollLeft;
    var focusY = screenY + document.body.scrollTop;
    var i, len;
    var msgDivs = document.getElementsByClassName("mail-message");
    var msgDiv, msgBodyDiv;
    var msgTop, msgDivTop, nextMsgTop;
    var initialH;
    var initialScale;
    var scaledOriginX, scaledOriginY;
    var translateX, translateY;
    var origin;

    gScaleInfo = undefined;

    for (i = 0, len = msgDivs.length; i < len; i++) {
        msgDiv = msgDivs[i];
        msgTop = nextMsgTop ? nextMsgTop : getTotalOffset(msgDiv).top;
        nextMsgTop = (i < len-1) ? getTotalOffset(msgDivs[i+1]).top : document.body.offsetHeight;
        if (focusY >= msgTop && focusY < nextMsgTop) {
            msgBodyDiv = msgDiv.children[1];
            initialScale = msgBodyDiv.getAttribute("data-initial-scale") || 1.0;

            msgDivTop = getTotalOffset(msgBodyDiv).top;

            // TODO: correct only for no initial translation
            // FIXME: wrong for initialScale > 1.0
            scaledOriginX = focusX / initialScale;
            scaledOriginY = (focusY - msgDivTop) / initialScale;

            // TODO: is this still needed?
            translateX = 0;
            translateY = 0;

            gScaleInfo = {
                div: msgBodyDiv,
                initialScale: initialScale,
                initialScreenX: screenX,
                initialScreenY: screenY,
                originX: scaledOriginX,
                originY: scaledOriginY,
                translateX: translateX,
                translateY: translateY,
                initialH: getCachedValue(msgBodyDiv, "offsetHeight", "data-initial-height"),
                minScale: Math.min(document.body.offsetWidth / msgBodyDiv.scrollWidth, 1.0),
                currScale: initialScale,
                currTranslateX: 0,
                currTranslateY: 0
            };

            origin = scaledOriginX + "px " + scaledOriginY + "px";
            msgBodyDiv.classList.add("zooming-focused");
            msgBodyDiv.style.webkitTransformOrigin = origin;
            msgBodyDiv.style.webkitTransform = "scale3d(" + initialScale + "," + initialScale
                + ",1) translate3d(" + translateX + "px," + translateY + "px,0)";
//            console.log("scaleBegin, h=" + gScaleInfo.initialH + " origin='" + origin + "'");
            break;
        }
    }
}

function onScaleEnd(screenX, screenY) {
    var msgBodyDiv;
    var scale;
    var h;
    if (!gScaleInfo) {
        return;
    }

//    console.log("JS got scaleEnd x/y=" + screenX + "/" + screenY);
    msgBodyDiv = gScaleInfo.div;
    scale = gScaleInfo.currScale;
    msgBodyDiv.style.webkitTransformOrigin = "0 0";
    // clear any translate
    // switching to a 2D transform here re-renders the fonts more clearly, but introduces
    // texture upload lag to any subsequent scale operation
    // TODO: conditionalize this based on device GPU performance and/or body size/complexity?
    if (true) {
        msgBodyDiv.style.webkitTransform = "scale(" + gScaleInfo.currScale + ")";
    } else {
        msgBodyDiv.style.webkitTransform = "scale3d(" + scale + "," + scale + ",1)";
    }
    h = gScaleInfo.initialH * scale;
//    console.log("onScaleEnd set h=" + h);
    msgBodyDiv.style.height = h + "px";

    // Use saved translateX/Y rather than calculating from screenX/Y because screenX/Y values
    // from onScaleEnd only track focus of remaining pointers, which is not useful and leads
    // to a perceived jump.
    var deltaScrollX = (scale - 1) * gScaleInfo.originX - gScaleInfo.currTranslateX;
    var deltaScrollY = (scale - 1) * gScaleInfo.originY - gScaleInfo.currTranslateY;
//    console.log("JS adjusting scroll by x/y=" + deltaScrollX + "/" + deltaScrollY);

    msgBodyDiv.classList.remove("zooming-focused");
    msgBodyDiv.setAttribute("data-initial-scale", scale);

    // TODO: is there a better way to make this more reliable?
    window.setTimeout(function() {
        window.scrollBy(deltaScrollX, deltaScrollY);
    }, 10);
}

function onScale(relativeScale, screenX, screenY) {
    var scale;
    var translateX, translateY;
    var transform;

    if (!gScaleInfo) {
        return;
    }

    scale = Math.max(gScaleInfo.initialScale * relativeScale, gScaleInfo.minScale);
    if (scale > 4.0) {
        scale = 4.0;
    }
    translateX = screenX - gScaleInfo.initialScreenX;
    translateY = screenY - gScaleInfo.initialScreenY;
    // TODO: clamp translation to prevent going beyond body edges
    gScaleInfo.currScale = scale;
    gScaleInfo.currTranslateX = translateX;
    gScaleInfo.currTranslateY = translateY;
    transform = "translate3d(" + translateX + "px," + translateY + "px,0) scale3d("
        + scale + "," + scale + ",1) translate3d(" + gScaleInfo.translateX + "px,"
        + gScaleInfo.translateY + "px,0)";
    gScaleInfo.div.style.webkitTransform = transform;
//    console.log("JS got scale=" + scale + " x/y=" + screenX + "/" + screenY
//        + " transform='" + transform + "'");
}

// END Java->JavaScript handlers

// Do this first to ensure that the readiness signal comes through,
// even if a stray exception later occurs.
setupContentReady();

collapseAllQuotedText();
hideAllUnsafeImages();
normalizeAllMessageWidths();
//setWideViewport();
restoreScrollPosition();
measurePositions();

