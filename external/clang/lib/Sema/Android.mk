LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES :=    \
	AttrList.inc	\
	Attrs.inc	\
	AttrParsedAttrKinds.inc    \
	AttrParsedAttrList.inc    \
	AttrSpellingListIndex.inc \
	AttrTemplateInstantiate.inc	\
	CommentCommandList.inc \
	CommentNodes.inc \
	DeclNodes.inc	\
	DiagnosticASTKinds.inc	\
	DiagnosticSemaKinds.inc	\
	DiagnosticParseKinds.inc	\
	DiagnosticCommentKinds.inc \
	DiagnosticCommonKinds.inc	\
	StmtNodes.inc	\
	arm_neon.inc

clang_sema_SRC_FILES :=	\
	AnalysisBasedWarnings.cpp	\
	AttributeList.cpp	\
	CodeCompleteConsumer.cpp	\
	DeclSpec.cpp	\
	IdentifierResolver.cpp	\
	DelayedDiagnostic.cpp \
	JumpDiagnostics.cpp	\
	MultiplexExternalSemaSource.cpp \
	Scope.cpp \
	ScopeInfo.cpp \
	Sema.cpp	\
	SemaAccess.cpp	\
	SemaAttr.cpp	\
	SemaCXXScopeSpec.cpp	\
	SemaCast.cpp \
	SemaChecking.cpp	\
	SemaCodeComplete.cpp	\
	SemaConsumer.cpp	\
	SemaDecl.cpp	\
	SemaDeclAttr.cpp	\
	SemaDeclCXX.cpp	\
	SemaDeclObjC.cpp	\
	SemaExceptionSpec.cpp	\
	SemaExpr.cpp	\
	SemaExprCXX.cpp	\
	SemaExprMember.cpp \
	SemaExprObjC.cpp	\
	SemaFixItUtils.cpp \
	SemaInit.cpp	\
	SemaLambda.cpp	\
	SemaLookup.cpp	\
	SemaObjCProperty.cpp	\
	SemaOpenMP.cpp	\
	SemaOverload.cpp	\
	SemaPseudoObject.cpp	\
	SemaStmt.cpp	\
        SemaStmtAsm.cpp \
        SemaStmtAttr.cpp \
	SemaTemplate.cpp	\
	SemaTemplateDeduction.cpp	\
	SemaTemplateInstantiate.cpp	\
	SemaTemplateInstantiateDecl.cpp	\
	SemaTemplateVariadic.cpp	\
	SemaType.cpp	\
	TargetAttributesSema.cpp	\
	TypeLocBuilder.cpp

LOCAL_SRC_FILES := $(clang_sema_SRC_FILES)

LOCAL_MODULE:= libclangSema
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
