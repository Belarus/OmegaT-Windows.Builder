#!/bin/sh
set -x

export PATH=/cygdrive/C/cygwin/bin:$PATH
#scp -Cp ../out/i18n-bel-win7.version.txt alex73@shell.berlios.de:/home/groups/i18n-bel/htdocs/
#ssh alex73@shell.berlios.de chmod 644 /home/groups/i18n-bel/htdocs/*.txt

scp -Cp ../out/i18n-bel-win7.version.txt ../out/win7bel.exe alex@mounik.org:public_html/windows/
ssh alex@mounik.org chmod 644 'public_html/windows/*'

read i
