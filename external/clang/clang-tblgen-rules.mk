###########################################################
## TableGen: Compile .td files to .inc.
###########################################################
ifeq ($(LOCAL_MODULE_CLASS),)
    LOCAL_MODULE_CLASS := STATIC_LIBRARIES
endif

ifneq ($(strip $(TBLGEN_TABLES)),)

intermediates := $(call local-intermediates-dir)

ifneq ($(findstring AttrDump.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/AttrDump.inc
$(intermediates)/include/clang/AST/AttrDump.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/AttrDump.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-dump)
endif

ifneq ($(findstring AttrImpl.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/AttrImpl.inc
$(intermediates)/include/clang/AST/AttrImpl.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/AttrImpl.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-impl)
endif

ifneq ($(findstring AttrList.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Basic/AttrList.inc
$(intermediates)/include/clang/Basic/AttrList.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Basic/AttrList.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-list)
endif

ifneq ($(findstring AttrSpellings.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Lex/AttrSpellings.inc
$(intermediates)/include/clang/Lex/AttrSpellings.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Lex/AttrSpellings.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-spelling-list)
endif

ifneq ($(findstring AttrSpellingListIndex.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Sema/AttrSpellingListIndex.inc
$(intermediates)/include/clang/Sema/AttrSpellingListIndex.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Sema/AttrSpellingListIndex.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-spelling-index)
endif

ifneq ($(findstring AttrPCHRead.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Serialization/AttrPCHRead.inc
$(intermediates)/include/clang/Serialization/AttrPCHRead.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Serialization/AttrPCHRead.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-pch-read)
endif

ifneq ($(findstring AttrPCHWrite.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Serialization/AttrPCHWrite.inc
$(intermediates)/include/clang/Serialization/AttrPCHWrite.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Serialization/AttrPCHWrite.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-pch-write)
endif

ifneq ($(findstring AttrExprArgs.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Parse/AttrExprArgs.inc
$(intermediates)/include/clang/Parse/AttrExprArgs.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Parse/AttrExprArgs.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-expr-args-list)
endif

ifneq ($(findstring AttrLateParsed.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Parse/AttrLateParsed.inc
$(intermediates)/include/clang/Parse/AttrLateParsed.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Parse/AttrLateParsed.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-late-parsed-list)
endif

ifneq ($(findstring Attrs.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/Attrs.inc
$(intermediates)/include/clang/AST/Attrs.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/Attrs.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-classes)
endif

ifneq ($(findstring AttrParsedAttrKinds.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Sema/AttrParsedAttrKinds.inc
$(intermediates)/include/clang/Sema/AttrParsedAttrKinds.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Sema/AttrParsedAttrKinds.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-parsed-attr-kinds)
endif

ifneq ($(findstring AttrParsedAttrList.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Sema/AttrParsedAttrList.inc
$(intermediates)/include/clang/Sema/AttrParsedAttrList.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Sema/AttrParsedAttrList.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-parsed-attr-list)
endif

ifneq ($(findstring AttrTemplateInstantiate.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Sema/AttrTemplateInstantiate.inc
$(intermediates)/include/clang/Sema/AttrTemplateInstantiate.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Sema/AttrTemplateInstantiate.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Attr.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-attr-template-instantiate)
endif

ifneq ($(findstring Checkers.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/Checkers.inc
$(intermediates)/Checkers.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/Checkers.inc: \
  $(CLANG_ROOT_PATH)/lib/StaticAnalyzer/Checkers/Checkers.td \
  $(CLANG_ROOT_PATH)/include/clang/StaticAnalyzer/Checkers/CheckerBase.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-sa-checkers)
endif

ifneq ($(findstring CommentCommandInfo.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentCommandInfo.inc
$(intermediates)/include/clang/AST/CommentCommandInfo.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentCommandInfo.inc: \
  $(CLANG_ROOT_PATH)/include/clang/AST/CommentCommands.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-command-info)
endif

ifneq ($(findstring CommentCommandList.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentCommandList.inc
$(intermediates)/include/clang/AST/CommentCommandList.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentCommandList.inc: \
  $(CLANG_ROOT_PATH)/include/clang/AST/CommentCommands.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-command-list)
endif

ifneq ($(findstring CommentHTMLNamedCharacterReferences.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentHTMLNamedCharacterReferences.inc
$(intermediates)/include/clang/AST/CommentHTMLNamedCharacterReferences.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentHTMLNamedCharacterReferences.inc: \
  $(CLANG_ROOT_PATH)/include/clang/AST/CommentHTMLNamedCharacterReferences.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-html-named-character-references)
endif

ifneq ($(findstring CommentHTMLTagsProperties.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentHTMLTagsProperties.inc
$(intermediates)/include/clang/AST/CommentHTMLTagsProperties.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentHTMLTagsProperties.inc: \
  $(CLANG_ROOT_PATH)/include/clang/AST/CommentHTMLTags.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-html-tags-properties)
endif

ifneq ($(findstring CommentHTMLTags.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentHTMLTags.inc
$(intermediates)/include/clang/AST/CommentHTMLTags.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentHTMLTags.inc: \
  $(CLANG_ROOT_PATH)/include/clang/AST/CommentHTMLTags.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-html-tags)
endif

ifneq ($(findstring CommentNodes.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/CommentNodes.inc
$(intermediates)/include/clang/AST/CommentNodes.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/CommentNodes.inc: \
  $(CLANG_ROOT_PATH)/include/clang/Basic/CommentNodes.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-comment-nodes)
endif

ifneq ($(filter Diagnostic%Kinds.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(addprefix $(intermediates)/include/clang/Basic/,$(filter Diagnostic%Kinds.inc,$(TBLGEN_TABLES)))
$(intermediates)/include/clang/Basic/Diagnostic%Kinds.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Basic/Diagnostic%Kinds.inc: \
  $(CLANG_ROOT_PATH)/include/clang/Basic/Diagnostic.td \
  $(CLANG_ROOT_PATH)/include/clang/Basic/Diagnostic%Kinds.td \
  | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-diags-defs -clang-component=$(patsubst Diagnostic%Kinds.inc,%,$(@F)))
endif

ifneq ($(findstring DiagnosticGroups.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Basic/DiagnosticGroups.inc
$(intermediates)/include/clang/Basic/DiagnosticGroups.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Basic/DiagnosticGroups.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Diagnostic.td $(CLANG_ROOT_PATH)/include/clang/Basic/DiagnosticGroups.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-diag-groups)
endif

ifneq ($(findstring DiagnosticIndexName.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Basic/DiagnosticIndexName.inc
$(intermediates)/include/clang/Basic/DiagnosticIndexName.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Basic/DiagnosticIndexName.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Diagnostic.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-diag-groups)
endif

ifneq ($(findstring DeclNodes.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/DeclNodes.inc
$(intermediates)/include/clang/AST/DeclNodes.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/DeclNodes.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/DeclNodes.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-decl-nodes)
endif

ifneq ($(findstring StmtNodes.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/AST/StmtNodes.inc
$(intermediates)/include/clang/AST/StmtNodes.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/AST/StmtNodes.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/StmtNodes.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,clang-stmt-nodes)
endif

ifneq ($(findstring arm_neon.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Basic/arm_neon.inc
$(intermediates)/include/clang/Basic/arm_neon.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Basic/arm_neon.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/arm_neon.td | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,arm-neon-sema)
endif

ifneq ($(findstring Options.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Driver/Options.inc
$(intermediates)/include/clang/Driver/Options.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Driver/Options.inc: $(CLANG_ROOT_PATH)/include/clang/Driver/Options.td $(LLVM_ROOT_PATH)/include/llvm/Option/OptParser.td $(CLANG_ROOT_PATH)/include/clang/Driver/CC1Options.td \
    | $(CLANG_TBLGEN) $(TBLGEN)
	$(call transform-host-td-to-out,opt-parser-defs)
endif

ifneq ($(findstring CC1AsOptions.inc,$(TBLGEN_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Driver/CC1AsOptions.inc
$(intermediates)/include/clang/Driver/CC1AsOptions.inc: TBLGEN_LOCAL_MODULE := $(LOCAL_MODULE)
$(intermediates)/include/clang/Driver/CC1AsOptions.inc: $(CLANG_ROOT_PATH)/include/clang/Driver/CC1AsOptions.td $(LLVM_ROOT_PATH)/include/llvm/Option/OptParser.td \
    | $(CLANG_TBLGEN) $(TBLGEN)
	$(call transform-host-td-to-out,opt-parser-defs)
endif

LOCAL_C_INCLUDES += $(intermediates)/include

endif
