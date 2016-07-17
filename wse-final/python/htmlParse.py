from bs4 import BeautifulSoup

#uses BeautifulSoup to clean up (remove invalid tags) and get the title-body text
class parseHTMLContents:
	def parse(self, html):
		soup = BeautifulSoup(html, "html.parser")
		#clean-up invalid HTML tags
		prettyHtmlDoc = soup.prettify()
		#get title, body, etc. text
		allText = soup.getText()
		return allText

