package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.ClientManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.IntegrityCheckView;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    private final IntegrityCheckView integrityCheckView;
    private final ClientManagementView clientManagementView;
    private final AuditLogManagementView auditLogManagementView;

    private SimplePanel contentPanel;
    private HTML integrityCheckMenuItem;
    private HTML clientManagementMenuItem;
    private HTML auditLogManagementMenuItem;
    private HTML currentActiveMenuItem;

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider,
            IntegrityCheckView integrityCheckView,
            ClientManagementView clientManagementView,
            AuditLogManagementView auditLogManagementView) {
        super(modelProvider);

        this.integrityCheckView = integrityCheckView;
        this.clientManagementView = clientManagementView;
        this.auditLogManagementView = auditLogManagementView;

        // 기본 테이블 숨김
        getTable().setVisible(false);

        // 1. 메인 컨테이너 생성 (CSS의 display: table 역할)
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().getStyle().setDisplay(Display.TABLE);
        mainContainer.getElement().getStyle().setWidth(100, Unit.PCT);
        mainContainer.getElement().getStyle().setHeight(100, Unit.PCT);
        mainContainer.getElement().getStyle().setProperty("minHeight", "600px"); //$NON-NLS-1$ //$NON-NLS-2$

        // 2. 사이드바 영역 생성 (display: table-cell)
        FlowPanel sidebar = new FlowPanel();
        sidebar.getElement().getStyle().setDisplay(Display.TABLE_CELL);
        sidebar.getElement().getStyle().setWidth(250, Unit.PX);
        sidebar.getElement().getStyle().setHeight(100, Unit.PCT);
        sidebar.getElement().getStyle().setVerticalAlign(VerticalAlign.TOP);
        sidebar.getElement().getStyle().setBackgroundColor("#ffffff"); //$NON-NLS-1$
        sidebar.getElement().getStyle().setProperty("borderRight", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$

        // 3. 사이드바 내부 Flex 컨테이너
        FlowPanel sidebarInner = new FlowPanel();
        sidebarInner.getElement().getStyle().setDisplay(Display.FLEX);
        sidebarInner.getElement().getStyle().setProperty("flexDirection", "column"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarInner.setHeight("100%"); //$NON-NLS-1$

        // 4. "보안 설정" 헤더
        HTML headerTitle = new HTML("보안 설정"); //$NON-NLS-1$
        headerTitle.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        headerTitle.getElement().getStyle().setFontSize(16, Unit.PX);
        headerTitle.getElement().getStyle().setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
        headerTitle.getElement().getStyle().setProperty("borderBottom", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        headerTitle.getElement().getStyle().setBackgroundColor("#f8f8f8"); //$NON-NLS-1$
        headerTitle.getElement().getStyle().setProperty("flexShrink", "0"); //$NON-NLS-1$ //$NON-NLS-2$

        // 5. 메뉴 리스트 영역 (여기에 메뉴 아이템 추가)
        FlowPanel menuList = new FlowPanel();
        menuList.getElement().getStyle().setProperty("flex", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        menuList.getElement().getStyle().setOverflowY(Overflow.AUTO);
        menuList.getElement().getStyle().setDisplay(Display.BLOCK);
        menuList.getElement().getStyle().setProperty("minHeight", "200px"); //$NON-NLS-1$ //$NON-NLS-2$

        // 6. 컨텐츠 영역 (display: table-cell)
        FlowPanel contentArea = new FlowPanel();
        contentArea.getElement().getStyle().setDisplay(Display.TABLE_CELL);
        contentArea.getElement().getStyle().setHeight(100, Unit.PCT);
        contentArea.getElement().getStyle().setVerticalAlign(VerticalAlign.TOP);
        contentArea.getElement().getStyle().setBackgroundColor("#ffffff"); //$NON-NLS-1$

        // 메뉴 아이템 생성 (스타일 설정은 기존과 동일)
        integrityCheckMenuItem = createMenuItem("무결성 검사"); //$NON-NLS-1$
        clientManagementMenuItem = createMenuItem("클라이언트 관리"); //$NON-NLS-1$
        auditLogManagementMenuItem = createMenuItem("감사기록 관리"); //$NON-NLS-1$

        // Content Panel 초기화
        contentPanel = new SimplePanel();
        contentPanel.getElement().setId("contentPanel"); //$NON-NLS-1$

        // 위젯 조립 (부모-자식 관계 설정)
        // 메뉴 리스트에 아이템 추가
        menuList.add(integrityCheckMenuItem);
        menuList.add(clientManagementMenuItem);
        menuList.add(auditLogManagementMenuItem);

        // 사이드바 조립
        sidebarInner.add(headerTitle);
        sidebarInner.add(menuList);
        sidebar.add(sidebarInner);

        // 컨텐츠 영역 조립
        contentArea.add(contentPanel);

        // 메인 컨테이너 조립
        mainContainer.add(sidebar);
        mainContainer.add(contentArea);

        // 최종적으로 테이블 래퍼에 추가
        getTable().getOuterWidget().add(mainContainer);

        // 핸들러 초기화 및 기본 탭 표시
        initializeHandlers();
        showIntegrityCheck();

        initWidget(getTable());
    }

    // 메뉴 아이템 생성 헬퍼 메소드 (중복 코드 제거)
    private HTML createMenuItem(String text) {
        HTML item = new HTML(text);
        item.setStyleName("security-menu-item"); //$NON-NLS-1$
        item.getElement().getStyle().setDisplay(Display.BLOCK);
        item.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setCursor(com.google.gwt.dom.client.Style.Cursor.POINTER);
        item.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("visibility", "visible"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("opacity", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        return item;
    }

    private void initializeHandlers() {
        integrityCheckMenuItem.addClickHandler(event -> showIntegrityCheck());
        clientManagementMenuItem.addClickHandler(event -> showClientManagement());
        auditLogManagementMenuItem.addClickHandler(event -> showAuditLogManagement());
    }

    private void showIntegrityCheck() {
        setActiveMenuItem(integrityCheckMenuItem);
        contentPanel.setWidget(integrityCheckView);
    }

    private void showClientManagement() {
        setActiveMenuItem(clientManagementMenuItem);
        contentPanel.setWidget(clientManagementView);
    }

    private void showAuditLogManagement() {
        setActiveMenuItem(auditLogManagementMenuItem);
        contentPanel.setWidget(auditLogManagementView);
    }

    private void setActiveMenuItem(HTML menuItem) {
        if (currentActiveMenuItem != null) {
            currentActiveMenuItem.getElement().getStyle().clearBackgroundColor();
            currentActiveMenuItem.getElement().getStyle().clearColor();
            currentActiveMenuItem.getElement().getStyle().clearFontWeight();
        }
        menuItem.getElement().getStyle().setBackgroundColor("#337ab7"); //$NON-NLS-1$
        menuItem.getElement().getStyle().setColor("white"); //$NON-NLS-1$
        menuItem.getElement().getStyle().setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
        currentActiveMenuItem = menuItem;
    }
}
