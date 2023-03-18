import os
import win32api
import win32com.client
import win32con
import win32com.client


# 获取桌面文件地址
def get_desktop():
    key = win32api.RegOpenKey(win32con.HKEY_CURRENT_USER,
                              r'Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders',
                              0,
                              win32con.KEY_READ)
    return win32api.RegQueryValueEx(key, 'Desktop')[0]


# 获取本机的应用安装路径
def get_folder_apply(filepath, app_name):
    """
    获取本机的应用安装路径
    :param filepath: 文件夹路径
    :param app_name: 应用名称
    :return: 应用安装地址
    """

    # 文件夹目录
    desk_path = filepath

    # 得到文件夹下的所有文件名称
    files = os.listdir(desk_path)

    apply_path = None
    for file in files:  # 遍历文件夹

        # 判断真实路径
        if ".lnk" in file:
            shell = win32com.client.Dispatch("WScript.Shell")
            shortcut = shell.CreateShortCut(desk_path + "\\" + file)
            paths = shortcut.Targetpath
            pathsArr = paths.split("\\")
            pathsName = pathsArr[len(pathsArr) - 1]

        # 判断快捷方式名称
        if app_name in file or app_name in pathsName:
            shell = win32com.client.Dispatch("WScript.Shell")
            shortcut = shell.CreateShortCut(desk_path + "\\" + file)
            apply_path = shortcut.Targetpath
            break

    return verify_apply(apply_path)


# 通过注册表获取本机安装应用的安装路径
def find_apply_path(upper_value_keyword, upper_item_name_keyword):
    """
    获取windows安装的应用
    :param upper_value_keyword: 注册表key名称
    :param upper_item_name_keyword: 应用名称
    :return:
    """

    # 应用安装地址
    path = None

    # 查找的注册表分支1
    sub_key1 = r'SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths'

    # 读取注册表
    key1 = win32api.RegOpenKey(win32con.HKEY_LOCAL_MACHINE, sub_key1, 0, win32con.KEY_READ)

    # 获取注册表
    info1 = win32api.RegQueryInfoKey(key1)

    # 遍历注册表
    for i in range(0, info1[0]):

        # 取出注册表key名称
        key_name = win32api.RegEnumKey(key1, i)

        # 去除特殊符号
        upper_key_name = key_name.strip()
        upper_item_name_keyword = upper_item_name_keyword.strip()

        # 注册表key名称对比
        if upper_key_name == upper_item_name_keyword or upper_key_name.upper() == upper_item_name_keyword:
            # 拼接注册表分支地址
            sub_key2 = sub_key1 + '\\' + key_name
            # 获取注册表分支详情
            key2 = win32api.RegOpenKey(win32con.HKEY_LOCAL_MACHINE, sub_key2, 0, win32con.KEY_READ)
            info2 = win32api.RegQueryInfoKey(key2)

            # 遍历取出注册表分支中的值
            for j in range(0, info2[1]):
                key_value = win32api.RegEnumValue(key2, j)[1]
                # 对比特征
                if key_value.upper().endswith(upper_value_keyword.upper()) or key_value.upper().endswith(
                        upper_value_keyword) or key_value.upper().endswith(upper_key_name):
                    path = key_value
                    break
            # 关闭注册表
            win32api.RegCloseKey(key2)
            break
    # 关闭注册表
    win32api.RegCloseKey(key1)
    return verify_apply(path)


# 验证文件是否存在
def verify_apply(apply_path):
    """
    验证文件是否存在
    :param apply_path:
    :return:
    """
    if apply_path is None:
        return None
    if os.path.exists(apply_path):
        return apply_path
    else:
        return None


# 获取浏览器
def get_browser():
    """
    获取本机浏览器信息
    :return:
    """
    # chrome 浏览器
    # Microsoft Edge 浏览器

    # 浏览器
    browser_name = ["chrome", "Microsoft Edge"]

    # 桌面快捷方式文件夹
    desktop_path = get_desktop()
    # 系统菜单文件夹
    file_path = 'C:\ProgramData\Microsoft\Windows\Start Menu\Programs'

    # 浏览器安装目录地址
    browser = {"chrome": None, "msedge": None}

    for browser_info_name in browser_name:
        # 通过注册表获取
        print(browser_info_name)

        # 谷歌浏览器
        if browser_info_name == 'chrome':
            # 注册表
            upper_value_keyword = 'chrome.exe'
            upper_item_name_keyword = 'chrome.exe'
            manager = find_apply_path(upper_value_keyword, upper_item_name_keyword)

            # 桌面快捷方式
            if manager is None:
                manager = get_folder_apply(desktop_path, browser_info_name)

            # 系统菜单快捷方式
            if manager is None:
                manager = get_folder_apply(file_path, browser_info_name)

            browser["chrome"] = manager

        # msedge 浏览器
        else:
            # 注册表
            upper_value_keyword = 'MSEDGE.EXE'
            upper_item_name_keyword = 'msedge.exe'
            manager = find_apply_path(upper_value_keyword, upper_item_name_keyword)

            apply_name = 'Microsoft Edge'
            # 桌面快捷方式
            if manager is None:
                manager = get_folder_apply(desktop_path, apply_name)

            # 系统菜单快捷方式
            if manager is None:
                manager = get_folder_apply(file_path, apply_name)

            browser["msedge"] = manager

    return browser


if __name__ == '__main__':
    exePath = get_browser()

    # msedge
    print(exePath.get("msedge"))
    # chrome
    print(exePath.get("chrome"))
