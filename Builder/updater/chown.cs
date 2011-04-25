using System;
using System.Text;
using System.IO;
using System.Security.Principal;
using System.Threading;
using System.Security.AccessControl;
using System.Windows.Forms;
using System.Diagnostics;

namespace Project1
{
    class chown
    {
        static void Main(string[] dirs)
        {
           
            try
            {
                foreach (string dir in dirs)
                {
                    setDirectoryAccess(dir);

                    if (!Directory.Exists(dir + "\\be-BY"))
                    {
                        Directory.CreateDirectory(dir + "\\be-BY");
                    }

                    setDirectoryAccess(dir + "\\be-BY");
                }
                Environment.Exit(0);
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message, "Памылка стварэньня каталёгаў", MessageBoxButtons.OK, MessageBoxIcon.Error);
                Environment.Exit(1);
            }
        }

        static void setDirectoryAccess(string dir)
        {
            WindowsIdentity me = WindowsIdentity.GetCurrent();
            SecurityIdentifier admins = new SecurityIdentifier(WellKnownSidType.BuiltinAdministratorsSid, null);
            FileSystemAccessRule ruleAdmins = new FileSystemAccessRule(admins, FileSystemRights.FullControl, AccessControlType.Allow);
            FileSystemAccessRule ruleMe = new FileSystemAccessRule(me.User, FileSystemRights.FullControl, AccessControlType.Allow);

            Process p = Process.Start("takeown.exe","/F \"" + dir+"\"");
            p.WaitForExit();
            if (p.ExitCode != 0)
            {
                throw new Exception("takeown can't be executed");
            }

            DirectoryInfo dInfo = new DirectoryInfo(dir);
            DirectorySecurity dSecurity = dInfo.GetAccessControl();
            dSecurity.AddAccessRule(ruleAdmins);
            dInfo.SetAccessControl(dSecurity);
        }
    }
}
