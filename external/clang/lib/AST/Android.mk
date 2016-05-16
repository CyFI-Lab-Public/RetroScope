LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES :=    \
	AttrDump.inc	\
	AttrImpl.inc	\
	AttrList.inc	\
	Attrs.inc	\
	CommentCommandInfo.inc \
	CommentCommandList.inc \
	CommentHTMLNamedCharacterReferences.inc \
	CommentHTMLTags.inc \
	CommentHTMLTagsProperties.inc \
	CommentNodes.inc \
	DeclNodes.inc	\
	DiagnosticASTKinds.inc	\
	DiagnosticCommentKinds.inc \
	DiagnosticCommonKinds.inc	\
	DiagnosticSemaKinds.inc	\
	StmtNodes.inc

clang_ast_SRC_FILES :=	\
	APValue.cpp	\
	ASTConsumer.cpp	\
	ASTContext.cpp	\
	ASTDiagnostic.cpp	\
	ASTDumper.cpp	\
	ASTImporter.cpp	\
	ASTTypeTraits.cpp \
	AttrImpl.cpp	\
	Comment.cpp \
	CommentBriefParser.cpp \
	CommentCommandTraits.cpp \
	CommentLexer.cpp \
	CommentParser.cpp \
	CommentSema.cpp \
	CXXInheritance.cpp	\
	Decl.cpp	\
	DeclarationName.cpp	\
	DeclBase.cpp	\
	DeclCXX.cpp	\
	DeclFriend.cpp	\
	DeclGroup.cpp	\
	DeclObjC.cpp	\
	DeclOpenMP.cpp	\
	DeclPrinter.cpp	\
	DeclTemplate.cpp	\
	DumpXML.cpp	\
	Expr.cpp	\
	ExprClassification.cpp	\
	ExprConstant.cpp	\
	ExprCXX.cpp	\
	ExternalASTSource.cpp	\
	InheritViz.cpp	\
	ItaniumCXXABI.cpp	\
	ItaniumMangle.cpp	\
	Mangle.cpp	\
	MangleNumberingContext.cpp \
	MicrosoftCXXABI.cpp	\
	MicrosoftMangle.cpp	\
	NestedNameSpecifier.cpp	\
        NSAPI.cpp       \
	ParentMap.cpp	\
	RecordLayout.cpp	\
	RecordLayoutBuilder.cpp	\
	RawCommentList.cpp \
	SelectorLocationsKind.cpp \
	Stmt.cpp	\
	StmtIterator.cpp	\
	StmtPrinter.cpp	\
	StmtProfile.cpp	\
	StmtViz.cpp	\
	TemplateBase.cpp	\
	TemplateName.cpp	\
	Type.cpp	\
	TypeLoc.cpp	\
	TypePrinter.cpp \
	VTTBuilder.cpp \
	VTableBuilder.cpp

LOCAL_SRC_FILES := $(clang_ast_SRC_FILES)

LOCAL_MODULE:= libclangAST
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
