import bs4
import re

with open("e:/bishe/管理员界面（后台）/管理端主页html.html", "r", encoding="utf-8") as f:
    html = f.read()

soup = bs4.BeautifulSoup(html, "html.parser")
home_content = soup.find("div", class_="home-content")

if home_content:
    with open("e:/bishe/salary-system/src/main/resources/static/home-content.html", "w", encoding="utf-8") as out:
        out.write(home_content.prettify())
    print("SUCCESS: home-content formatted and saved.")
else:
    print("ERROR: class='home-content' not found.")
