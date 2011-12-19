using System;
using System.Collections.Generic;
using System.Text;
using System.Net;
using System.Text.RegularExpressions;
using System.IO;
using System.Windows.Forms;
using System.Diagnostics;

namespace win7bel_updater
{
    class Program
    {
        static Uri VERSION_URL = new Uri("http://windows.mounik.org/i18n-bel-win7.version.txt");
        static Uri INSTALLER_URL = new Uri("http://windows.mounik.org/win7bel.exe");

        static String instDir;
        static void Main()
        {
            instDir = Environment.GetEnvironmentVariable("PROGRAMFILES") + "\\win7bel\\";
            try
            {
                String remote = getRemoteVersion();
                String local = getLocalVersion();
                if (local.Equals(remote))
                {
                    return;
                }
            }
            catch (Exception)
            {
				// error request new version
				return;
            }

            DialogResult result = MessageBox.Show("На сайце http://mounik.org ёсць больш позняя версія перакладу Windows 7. Усталяваць яе зараз ?", "Беларускі пераклад Windows 7", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
            if (result == DialogResult.Yes)
            {
                try
                {
                    installNewVersion();
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message, "Памылка ўсталёўкі перакладу Windows 7", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    Environment.Exit(1);
                }
            }
            Environment.Exit(0);
        }

        static String getRemoteVersion()
        {
            WebClient client = new WebClient();
            client.Encoding = Encoding.UTF8;
            return client.DownloadString(VERSION_URL);
        }

        static String getLocalVersion()
        {
            String versionFile = instDir + "version.txt";
            StreamReader sr = new StreamReader(versionFile, Encoding.UTF8);
            return sr.ReadToEnd();
        }
        static void installNewVersion()
        {
            String installFile = Environment.GetEnvironmentVariable("TEMP") + "\\win7bel.exe";

            WebClient client = new WebClient();
            client.DownloadFile(INSTALLER_URL, installFile);
            Process.Start(installFile);
        }
    }
}
